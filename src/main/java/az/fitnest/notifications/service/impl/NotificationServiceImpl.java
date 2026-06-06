package az.fitnest.notifications.service.impl;

import az.fitnest.notifications.model.enums.NotificationStatus;

import az.fitnest.notifications.grpc.IdentityGrpcClient;
import az.fitnest.notifications.dto.NotificationDto;
import az.fitnest.notifications.dto.PushResult;
import az.fitnest.notifications.mapper.NotificationMapper;
import az.fitnest.notifications.model.entity.Device;
import az.fitnest.notifications.model.entity.Notification;
import az.fitnest.notifications.repository.DeviceRepository;
import az.fitnest.notifications.repository.NotificationRepository;
import az.fitnest.notifications.service.LsimSmsService;
import az.fitnest.notifications.service.NotificationService;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import az.fitnest.notifications.exception.ResourceNotFoundException;
import az.fitnest.notifications.model.enums.Platform;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final LsimSmsService lsimSmsService;
    private final DeviceRepository deviceRepository;
    private final NotificationRepository notificationRepository;
    private final Optional<FirebaseMessaging> firebaseMessaging;
    private final IdentityGrpcClient identityGrpcClient;
    private final java.util.concurrent.Executor taskExecutor;

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private NotificationServiceImpl self;

    public void sendWelcomeSms(String phoneNumber) {
        String message = "Xidmətimizə xoş gəlmisiniz!";
        lsimSmsService.sendSms(phoneNumber, message);
    }

    @Transactional
    public void registerDevice(Long userId, String pushToken, Platform platform) {
        if (platform == null) {
            throw new IllegalArgumentException("Platform must be specified and valid");
        }

        Optional<Device> existingDeviceOpt = deviceRepository.findByPushToken(pushToken);

        // Fetch user's previous current device's notification enabled status to inherit preference
        boolean notificationEnabled = true;
        Optional<Device> previousCurrentDeviceOpt = deviceRepository.findFirstByUserIdAndIsCurrentTrue(userId);
        if (previousCurrentDeviceOpt.isPresent()) {
            notificationEnabled = Boolean.TRUE.equals(previousCurrentDeviceOpt.get().getNotificationEnabled());
        }

        try {
            Device device;
            if (existingDeviceOpt.isPresent()) {
                device = existingDeviceOpt.get();
                device.setUserId(userId);
                device.setPlatform(platform);
                device.setIsCurrent(true);
                device.setNotificationEnabled(notificationEnabled);
            } else {
                device = new Device();
                device.setUserId(userId);
                device.setPushToken(pushToken);
                device.setPlatform(platform);
                device.setCreatedAt(LocalDateTime.now());
                device.setIsCurrent(true);
                device.setNotificationEnabled(notificationEnabled);
            }
            deviceRepository.save(device);

            // Deactivate all other devices in a single query
            deviceRepository.deactivateOtherDevices(userId, pushToken);
        } catch (DataIntegrityViolationException e) {
            logger.error("Device registration failed for user {}: {}", userId, e.getMessage());
        }
    }

    @Transactional
    public PushResult sendPushToUser(Long userId, String title, String body, Map<String, String> data) {
        if (firebaseMessaging.isEmpty()) {
            logger.warn("Firebase is not initialized. Cannot send push notification to user: {}", userId);
            return PushResult.builder().notificationId(-1L).build();
        }

        try {
            String sessionStatus = identityGrpcClient.getUserSessionStatus(userId);
            if (!"HAVE_SESSIONS".equals(sessionStatus)) {
                return PushResult.builder().notificationId(-1L).build();
            }
        } catch (Exception e) {
        }

        Notification notification = savePendingNotification(userId, title, body);
        Long notificationId = notification.getId();

        List<String> tokens = deviceRepository.findPushTokensByUserId(userId);
        if (tokens.isEmpty()) {
            updateNotificationStatus(notificationId, NotificationStatus.FAILED, 0, 0, "No registered devices");
            return PushResult.builder().notificationId(notificationId).build();
        }

        Map<String, String> payload = data != null ? data : Collections.emptyMap();

        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(payload)
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setNotification(AndroidNotification.builder()
                                .setDefaultSound(true)
                                .setDefaultVibrateTimings(true)
                                .build())
                        .build())
                .setApnsConfig(ApnsConfig.builder()
                        .setAps(Aps.builder()
                                .setSound("default")
                                .setBadge(1)
                                .setContentAvailable(true)
                                .build())
                        .build())
                .build();

        int sentCount = 0;
        int failedCount = 0;
        List<String> failedTokensToRemove = new ArrayList<>();
        String failureReason = null;

        try {
            BatchResponse response = firebaseMessaging.get().sendEachForMulticast(message);
            sentCount = response.getSuccessCount();
            failedCount = response.getFailureCount();

            if (failedCount > 0) {
                List<SendResponse> responses = response.getResponses();
                for (int i = 0; i < responses.size(); i++) {
                    SendResponse sendResponse = responses.get(i);
                    if (!sendResponse.isSuccessful()) {
                        FirebaseMessagingException e = sendResponse.getException();
                        String errorCode = e.getMessagingErrorCode().name();

                        if ("UNREGISTERED".equals(errorCode) || "INVALID_ARGUMENT".equals(errorCode)) {
                            failedTokensToRemove.add(tokens.get(i));
                        }
                    }
                }
            }
        } catch (FirebaseMessagingException e) {
            failedCount = tokens.size();
            failureReason = e.getMessage();
        } catch (Exception e) {
            failedCount = tokens.size();
            failureReason = e.getMessage();
        }

        int removedTokens = failedTokensToRemove.size();
        cleanupTokensAndUpdateStatus(userId, notificationId, failedTokensToRemove, sentCount, failedCount, failureReason);

        return PushResult.builder()
                .notificationId(notificationId)
                .sentCount(sentCount)
                .failedCount(failedCount)
                .removedTokens(removedTokens)
                .build();
    }

    @Transactional
    public void broadcastPushNotification(String title, String body) {
        if (firebaseMessaging.isEmpty()) {
            logger.warn("Firebase is not initialized. Cannot send broadcast push notification.");
            return;
        }

        List<String> tokens = deviceRepository.findAllPushTokens();
        if (tokens.isEmpty()) {
            return;
        }

        Map<String, String> payload = Collections.emptyMap();

        List<Long> userIds = deviceRepository.findAll().stream()
                .map(Device::getUserId)
                .distinct()
                .toList();

        for (Long userIdForNotification : userIds) {
            savePendingNotification(userIdForNotification, title, body);
        }

        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(payload)
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setNotification(AndroidNotification.builder()
                                .setDefaultSound(true)
                                .setDefaultVibrateTimings(true)
                                .build())
                        .build())
                .setApnsConfig(ApnsConfig.builder()
                        .setAps(Aps.builder()
                                .setSound("default")
                                .setBadge(1)
                                .setContentAvailable(true)
                                .build())
                        .build())
                .build();

        try {
            BatchResponse response = firebaseMessaging.get().sendEachForMulticast(message);
        } catch (FirebaseMessagingException e) {
            logger.error("Firebase messaging error during broadcast: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error during broadcast push notification: {}", e.getMessage(), e);
        }
    }

    public List<PushResult> sendPushToUsers(List<Long> userIds, String title, String body, Map<String, String> data) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<java.util.concurrent.CompletableFuture<PushResult>> futures = userIds.stream()
                .map(userId -> java.util.concurrent.CompletableFuture.supplyAsync(
                        () -> self.sendPushToUser(userId, title, body, data), taskExecutor))
                .toList();

        java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0])).join();

        return futures.stream()
                .map(java.util.concurrent.CompletableFuture::join)
                .toList();
    }

    @Transactional
    protected Notification savePendingNotification(Long userId, String title, String body) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setRead(false);
        notification.setStatus(NotificationStatus.PENDING);
        return notificationRepository.save(notification);
    }

    @Transactional
    protected void updateNotificationStatus(Long notificationId, NotificationStatus status, int sentCount, int failedCount, String failureReason) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setStatus(status);
            notification.setSentCount(sentCount);
            notification.setFailedCount(failedCount);
            notification.setFailureReason(failureReason);
        });
    }

    @Transactional
    protected void cleanupTokensAndUpdateStatus(Long userId, Long notificationId, List<String> staleTokens, int sentCount, int failedCount, String failureReason) {
        for (String token : staleTokens) {
            deviceRepository.deleteByPushToken(token);
        }

        NotificationStatus finalStatus = NotificationStatus.FAILED;
        if (sentCount > 0 && failedCount == 0) {
            finalStatus = NotificationStatus.SENT;
        } else if (sentCount > 0 && failedCount > 0) {
            finalStatus = NotificationStatus.PARTIAL;
        }

        updateNotificationStatus(notificationId, finalStatus, sentCount, failedCount, failureReason);
    }

    public void sendPushNotification(String token, String title, String body) {
        sendPushNotification(token, title, body, Collections.emptyMap());
    }

    public void sendPushNotification(String token, String title, String body, Map<String, String> data) {
        if (firebaseMessaging.isEmpty()) {
            logger.warn("Firebase is not initialized. Cannot send push notification to token: {}", maskToken(token));
            return;
        }

        try {
            Map<String, String> payload = data != null ? data : Collections.emptyMap();
            com.google.firebase.messaging.Message message = com.google.firebase.messaging.Message.builder()
                    .setToken(token)
                    .setNotification(com.google.firebase.messaging.Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putAllData(payload)
                    .build();

            String response = firebaseMessaging.get().send(message);
        } catch (FirebaseMessagingException e) {
            String errorCode = e.getMessagingErrorCode().name();
            if ("UNREGISTERED".equals(errorCode) || "INVALID_ARGUMENT".equals(errorCode)) {
                deviceRepository.deleteByPushToken(token);
            }
        }
    }

    @Transactional
    public void sendToDevice(Long deviceId, String title, String body) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Cihaz tapılmadı"));

        savePendingNotification(device.getUserId(), title, body);

        if (Boolean.TRUE.equals(device.getNotificationEnabled())) {
            sendPushNotification(device.getPushToken(), title, body, Collections.emptyMap());
        } else {
            logger.info("Skipping push notification to device {} because notifications are disabled.", deviceId);
        }
    }

    public Page<NotificationDto> getUserNotifications(Long userId, Pageable pageable) {
        if (userId == null) {
            return Page.empty(pageable);
        }

        Pageable finalPageable = pageable;
        if (pageable.getSort().isSorted()) {
            finalPageable = org.springframework.data.domain.PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    org.springframework.data.domain.Sort.by(
                            pageable.getSort().stream()
                                    .map(order -> {
                                        if ("createdAt".equals(order.getProperty())) {
                                            return order.isAscending() ?
                                                    org.springframework.data.domain.Sort.Order.asc("createdDate") :
                                                    org.springframework.data.domain.Sort.Order.desc("createdDate");
                                        }
                                        return order;
                                    })
                                    .toList()
                    )
            );
        }

        return notificationRepository.findAllByUserIdOrderByCreatedDateDesc(userId, finalPageable)
                .map(NotificationMapper::toDto);
    }

    @Transactional
    public void markNotificationAsRead(Long id, Long userId) {
        Notification notification = notificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Bildiriş tapılmadı"));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllNotificationsAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    @Transactional
    public void deleteNotification(Long id, Long userId) {
        notificationRepository.deleteByIdAndUserId(id, userId);
    }

    @Transactional
    public void deleteAllNotifications(Long userId) {
        notificationRepository.deleteAllByUserId(userId);
    }

    public List<Device> getDevicesByUserId(Long userId) {
        return deviceRepository.findAllByUserId(userId);
    }

    private String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 4) + "***" + token.substring(token.length() - 4);
    }

    @Override
    public void saveDevice(Device device) {
        deviceRepository.save(device);
    }

    @Override
    public int getUnreadCount(Long userId) {
        if (userId == null) {
            return 0;
        }
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }
}
