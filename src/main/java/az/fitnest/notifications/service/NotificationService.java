package az.fitnest.notifications.service;

import az.fitnest.notifications.dto.DeviceRegistrationRequest;
import az.fitnest.notifications.dto.NotificationDto;
import az.fitnest.notifications.dto.PushResult;
import az.fitnest.notifications.entity.Device;
import az.fitnest.notifications.entity.Notification;
import az.fitnest.notifications.entity.NotificationStatus;
import az.fitnest.notifications.repository.DeviceRepository;
import az.fitnest.notifications.repository.NotificationRepository;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {
    private final LsimSmsService lsimSmsService;
    private final DeviceRepository deviceRepository;
    private final NotificationRepository notificationRepository;
    private final FirebaseMessaging firebaseMessaging;

    public void sendWelcomeSms(String phoneNumber) {
        String message = "Xidmətimizə xoş gəlmisiniz!";
        Long transactionId = lsimSmsService.sendSms(phoneNumber, message);
        log.info("SMS sent, transaction ID: {}", transactionId);
    }

    @Transactional
    public void registerDevice(Long userId, DeviceRegistrationRequest request) {
        try {
            deviceRepository.findByPushToken(request.getPushToken())
                    .ifPresentOrElse(
                            device -> {
                                device.setUserId(userId);
                                device.setPlatform(request.getPlatform());
                                deviceRepository.save(device);
                                log.info("Updated device token for user {}", userId);
                            },
                            () -> {
                                Device device = new Device();
                                device.setUserId(userId);
                                device.setPushToken(request.getPushToken());
                                device.setPlatform(request.getPlatform());
                                device.setCreatedAt(LocalDateTime.now());
                                deviceRepository.save(device);
                                log.info("Registered new device token for user {}", userId);
                            }
                    );
        } catch (DataIntegrityViolationException e) {
            log.warn("Device token {} already registered concurrently, ignoring.", maskToken(request.getPushToken()));
        }
    }

    public PushResult sendPushToUser(Long userId, String title, String body, Map<String, String> data) {
        // 1. Save Pending Notification in a transaction to guarantee it's recorded
        Notification notification = savePendingNotification(userId, title, body);
        Long notificationId = notification.getId();
        
        // 2. Fetch device tokens
        List<String> tokens = deviceRepository.findPushTokensByUserId(userId);
        if (tokens.isEmpty()) {
            log.info("No devices registered for user {}, marking notification {} as failed.", userId, notificationId);
            updateNotificationStatus(notificationId, NotificationStatus.FAILED, 0, 0, "No registered devices");
            return PushResult.builder().notificationId(notificationId).build();
        }

        Map<String, String> payload = data != null ? data : Collections.emptyMap();
        
        // 3. Build Multicast Message with Platform Specific Configs
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

        // 4. Send the message via Firebase
        int sentCount = 0;
        int failedCount = 0;
        List<String> failedTokensToRemove = new ArrayList<>();
        String failureReason = null;

        try {
            BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
            sentCount = response.getSuccessCount();
            failedCount = response.getFailureCount();

            if (failedCount > 0) {
                List<SendResponse> responses = response.getResponses();
                for (int i = 0; i < responses.size(); i++) {
                    SendResponse sendResponse = responses.get(i);
                    if (!sendResponse.isSuccessful()) {
                        FirebaseMessagingException e = sendResponse.getException();
                        String errorCode = e.getMessagingErrorCode().name();
                        log.warn("Failed to send push to token {} for user {}: {}", maskToken(tokens.get(i)), userId, errorCode);
                        
                        if ("UNREGISTERED".equals(errorCode) || "INVALID_ARGUMENT".equals(errorCode)) {
                            failedTokensToRemove.add(tokens.get(i));
                        }
                    }
                }
            }
            log.info("Push multicasted to {} tokens for user {}. Success: {}, Failed: {}", tokens.size(), userId, sentCount, failedCount);
        } catch (FirebaseMessagingException e) {
            log.error("Fatal error sending push multicast to user {}: ", userId, e);
            failedCount = tokens.size();
            failureReason = e.getMessage();
        } catch (Exception e) {
            log.error("Unexpected error sending push to user {}: ", userId, e);
            failedCount = tokens.size();
            failureReason = e.getMessage();
        }

        // 5. Cleanup stale tokens and update the DB status
        int removedTokens = failedTokensToRemove.size();
        cleanupTokensAndUpdateStatus(notificationId, failedTokensToRemove, sentCount, failedCount, failureReason);

        return PushResult.builder()
                .notificationId(notificationId)
                .sentCount(sentCount)
                .failedCount(failedCount)
                .removedTokens(removedTokens)
                .build();
    }

    @Transactional
    public void broadcastPushNotification(String title, String body, Map<String, String> data) {
        List<String> tokens = deviceRepository.findAllPushTokens();
        if (tokens.isEmpty()) {
            log.info("No devices registered for broadcast");
            return;
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

        try {
            BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
            log.info("Broadcast push sent to {} devices. Success: {}, Failed: {}", 
                    tokens.size(), response.getSuccessCount(), response.getFailureCount());
        } catch (FirebaseMessagingException e) {
            log.error("Fatal error during broadcast push: ", e);
        } catch (Exception e) {
            log.error("Unexpected error during broadcast push: ", e);
        }
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
    protected void cleanupTokensAndUpdateStatus(Long notificationId, List<String> staleTokens, int sentCount, int failedCount, String failureReason) {
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

            String response = firebaseMessaging.send(message);
            log.info("Successfully sent simple push notification to token {}: {}", maskToken(token), response);
        } catch (FirebaseMessagingException e) {
            log.error("Error sending push notification to token {}: ", maskToken(token), e);
            String errorCode = e.getMessagingErrorCode().name();
            if ("UNREGISTERED".equals(errorCode) || "INVALID_ARGUMENT".equals(errorCode)) {
                log.info("Removing stale token: {}", maskToken(token));
                deviceRepository.deleteByPushToken(token);
            }
        }
    }

    public Page<NotificationDto> getUserNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findAllByUserIdOrderByCreatedDateDesc(userId, pageable)
                .map(notification -> NotificationDto.builder()
                        .id(notification.getId())
                        .title(notification.getTitle())
                        .body(notification.getBody())
                        .isRead(notification.isRead())
                        .createdAt(notification.getCreatedDate())
                        .build());
    }

    private String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 4) + "***" + token.substring(token.length() - 4);
    }
}