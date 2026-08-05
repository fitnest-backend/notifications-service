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
import az.fitnest.notifications.exception.BadRequestException;
import az.fitnest.notifications.exception.ResourceNotFoundException;
import az.fitnest.notifications.model.enums.Platform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);

    /** Number of users pushed concurrently per batch. Keeps in-flight tasks below the
     *  executor's capacity so large bulk sends (hundreds of users) never overwhelm the pool. */
    private static final int PUSH_BATCH_SIZE = 50;

    /** FCM multicast hard limit. */
    private static final int FCM_MULTICAST_LIMIT = 500;

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
        if (userId == null) {
            throw new BadRequestException("User id is required");
        }
        if (platform == null) {
            throw new BadRequestException("Platform must be specified and valid");
        }
        if (pushToken == null || pushToken.isBlank()) {
            throw new BadRequestException("Push token must not be blank");
        }

        final String token = pushToken.trim();

        // Inherit preference from this user's current device (first register → enabled)
        boolean notificationEnabled = true;
        Optional<Device> previousCurrentDeviceOpt = deviceRepository.findFirstByUserIdAndIsCurrentTrue(userId);
        if (previousCurrentDeviceOpt.isPresent()) {
            notificationEnabled = Boolean.TRUE.equals(previousCurrentDeviceOpt.get().getNotificationEnabled());
        }

        try {
            upsertCurrentDevice(userId, token, platform, notificationEnabled);
        } catch (DataIntegrityViolationException first) {
            // Concurrent insert of the same token — retry once as an update of the winning row
            logger.warn("Device registration race for user {}, retrying upsert: {}", userId, first.getMessage());
            try {
                upsertCurrentDevice(userId, token, platform, notificationEnabled);
            } catch (DataIntegrityViolationException second) {
                logger.error("Device registration failed for user {} after retry: {}", userId, second.getMessage());
                throw new BadRequestException("Device registration failed; please retry");
            }
        }
    }

    private void upsertCurrentDevice(Long userId, String token, Platform platform, boolean notificationEnabled) {
        Device device = resolveCanonicalDeviceByToken(token);
        Long previousOwnerId = null;

        if (device != null) {
            if (!userId.equals(device.getUserId())) {
                previousOwnerId = device.getUserId();
            }
            device.setUserId(userId);
            device.setPlatform(platform);
            device.setIsCurrent(true);
            device.setNotificationEnabled(notificationEnabled);
        } else {
            device = new Device();
            device.setUserId(userId);
            device.setPushToken(token);
            device.setPlatform(platform);
            device.setIsCurrent(true);
            device.setNotificationEnabled(notificationEnabled);
        }

        deviceRepository.saveAndFlush(device);
        deviceRepository.deactivateOtherDevices(userId, token);

        if (previousOwnerId != null) {
            restoreCurrentDeviceForUser(previousOwnerId);
        }
    }

    /**
     * Returns the newest row for a token and hard-deletes any duplicate rows.
     */
    private Device resolveCanonicalDeviceByToken(String token) {
        List<Device> matches = deviceRepository.findAllByPushTokenOrderByNewest(token);
        if (matches.isEmpty()) {
            return null;
        }
        Device canonical = matches.get(0);
        for (int i = 1; i < matches.size(); i++) {
            deviceRepository.delete(matches.get(i));
        }
        if (matches.size() > 1) {
            deviceRepository.flush();
            logger.warn("Deduped {} duplicate device row(s) for push token {}", matches.size() - 1, maskToken(token));
        }
        return canonical;
    }

    /**
     * After a token moves to another user, ensure the previous owner still has a current device
     * if they have remaining rows (without forcibly re-enabling notifications).
     */
    private void restoreCurrentDeviceForUser(Long previousOwnerId) {
        if (deviceRepository.findFirstByUserIdAndIsCurrentTrue(previousOwnerId).isPresent()) {
            return;
        }
        deviceRepository.findFirstByUserIdOrderByCreatedAtDesc(previousOwnerId).ifPresent(device -> {
            device.setIsCurrent(true);
            deviceRepository.save(device);
            logger.info("Promoted device {} as current for previous owner {}", device.getDeviceId(), previousOwnerId);
        });
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
        MulticastSendResult sendResult = sendMulticastInChunks(tokens, title, body, payload);

        cleanupTokensAndUpdateStatus(
                userId,
                notificationId,
                sendResult.staleTokens(),
                sendResult.sentCount(),
                sendResult.failedCount(),
                sendResult.failureReason());

        return PushResult.builder()
                .notificationId(notificationId)
                .sentCount(sendResult.sentCount())
                .failedCount(sendResult.failedCount())
                .removedTokens(sendResult.staleTokens().size())
                .build();
    }

    @Transactional
    public void broadcastPushNotification(String title, String body) {
        if (firebaseMessaging.isEmpty()) {
            logger.warn("Firebase is not initialized. Cannot send broadcast push notification.");
            return;
        }

        List<Long> userIds = deviceRepository.findUserIdsWithActivePushEnabled();
        for (Long userIdForNotification : userIds) {
            savePendingNotification(userIdForNotification, title, body);
        }

        List<String> tokens = deviceRepository.findAllPushTokens();
        if (tokens.isEmpty()) {
            return;
        }

        MulticastSendResult sendResult = sendMulticastInChunks(tokens, title, body, Collections.emptyMap());
        if (!sendResult.staleTokens().isEmpty()) {
            for (String staleToken : sendResult.staleTokens()) {
                deviceRepository.deleteByPushToken(staleToken);
            }
            logger.info("Broadcast removed {} stale push token(s)", sendResult.staleTokens().size());
        }
        logger.info("Broadcast finished: sent={}, failed={}, users={}",
                sendResult.sentCount(), sendResult.failedCount(), userIds.size());
    }

    @Override
    public int broadcastLocalizedPushNotification(Map<String, LocalizedPushContent> contentsByLanguage,
                                                  Map<String, String> data,
                                                  List<String> roleNames) {
        if (contentsByLanguage == null || contentsByLanguage.isEmpty()) {
            logger.warn("Localized broadcast skipped: no contents provided");
            return 0;
        }

        Map<String, LocalizedPushContent> normalizedContents = new HashMap<>();
        contentsByLanguage.forEach((lang, content) -> {
            if (lang != null && content != null) {
                normalizedContents.put(lang.trim().toUpperCase(Locale.ROOT), content);
            }
        });

        LocalizedPushContent fallback = normalizedContents.getOrDefault("AZ",
                normalizedContents.values().iterator().next());

        List<IdentityGrpcClient.ActiveUserLanguageDto> users;
        try {
            users = identityGrpcClient.getActiveUsersWithLanguage(roleNames);
        } catch (Exception e) {
            logger.error("Failed to fetch active users for localized broadcast: {}", e.getMessage(), e);
            return 0;
        }

        if (users.isEmpty()) {
            return 0;
        }

        // Only users with a current device + notifications enabled receive the fan-out
        java.util.Set<Long> pushEligibleUserIds = new java.util.HashSet<>(
                deviceRepository.findUserIdsWithActivePushEnabled());
        if (pushEligibleUserIds.isEmpty()) {
            logger.info("Localized broadcast skipped: no users with active push-enabled devices");
            return 0;
        }

        Map<String, List<Long>> userIdsByLanguage = new HashMap<>();
        for (IdentityGrpcClient.ActiveUserLanguageDto user : users) {
            if (!pushEligibleUserIds.contains(user.userId())) {
                continue;
            }
            String lang = normalizeLanguage(user.language());
            userIdsByLanguage.computeIfAbsent(lang, key -> new ArrayList<>()).add(user.userId());
        }

        if (userIdsByLanguage.isEmpty()) {
            logger.info("Localized broadcast skipped: no ROLE_USER overlap with push-enabled devices");
            return 0;
        }

        Map<String, String> payload = data != null ? data : Collections.emptyMap();
        int targetUsers = 0;

        for (Map.Entry<String, List<Long>> entry : userIdsByLanguage.entrySet()) {
            LocalizedPushContent content = normalizedContents.getOrDefault(entry.getKey(), fallback);
            deliverToUsersWithoutSessionGate(entry.getValue(), content.title(), content.body(), payload);
            targetUsers += entry.getValue().size();
        }

        logger.info("Localized broadcast completed for {} push-eligible users across {} language groups",
                targetUsers, userIdsByLanguage.size());
        return targetUsers;
    }

    private String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return "AZ";
        }
        String normalized = language.trim().toUpperCase(Locale.ROOT);
        if ("EN".equals(normalized) || "RU".equals(normalized) || "AZ".equals(normalized)) {
            return normalized;
        }
        return "AZ";
    }

    /**
     * Delivers in-app notifications to all users and sends push when Firebase + devices exist.
     * Unlike {@link #sendPushToUser}, this does not require HAVE_SESSIONS (marketing / system broadcasts).
     */
    private void deliverToUsersWithoutSessionGate(List<Long> userIds, String title, String body, Map<String, String> data) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        for (int start = 0; start < userIds.size(); start += PUSH_BATCH_SIZE) {
            int end = Math.min(start + PUSH_BATCH_SIZE, userIds.size());
            List<Long> batch = userIds.subList(start, end);

            List<java.util.concurrent.CompletableFuture<Void>> futures = batch.stream()
                    .map(userId -> java.util.concurrent.CompletableFuture.runAsync(
                            () -> self.deliverNotificationIgnoringSession(userId, title, body, data), taskExecutor))
                    .toList();

            java.util.concurrent.CompletableFuture.allOf(
                    futures.toArray(new java.util.concurrent.CompletableFuture[0])).join();
        }
    }

    @Transactional
    public void deliverNotificationIgnoringSession(Long userId, String title, String body, Map<String, String> data) {
        Notification notification = savePendingNotification(userId, title, body);
        Long notificationId = notification.getId();

        if (firebaseMessaging.isEmpty()) {
            updateNotificationStatus(notificationId, NotificationStatus.SENT, 0, 0, "Push service inactive; in-app only");
            return;
        }

        List<String> tokens = deviceRepository.findPushTokensByUserId(userId);
        if (tokens.isEmpty()) {
            updateNotificationStatus(notificationId, NotificationStatus.SENT, 0, 0, "No registered devices; in-app only");
            return;
        }

        Map<String, String> payload = data != null ? data : Collections.emptyMap();
        MulticastSendResult sendResult = sendMulticastInChunks(tokens, title, body, payload);
        cleanupTokensAndUpdateStatus(
                userId,
                notificationId,
                sendResult.staleTokens(),
                sendResult.sentCount(),
                sendResult.failedCount(),
                sendResult.failureReason());
    }

    public List<PushResult> sendPushToUsers(List<Long> userIds, String title, String body, Map<String, String> data) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<PushResult> results = new java.util.ArrayList<>(userIds.size());

        // Process the recipients in bounded batches ("part by part") so we never submit more
        // tasks at once than the async pool can absorb. Each batch is dispatched concurrently
        // and fully awaited before the next batch starts.
        for (int start = 0; start < userIds.size(); start += PUSH_BATCH_SIZE) {
            int end = Math.min(start + PUSH_BATCH_SIZE, userIds.size());
            List<Long> batch = userIds.subList(start, end);

            List<java.util.concurrent.CompletableFuture<PushResult>> futures = batch.stream()
                    .map(userId -> java.util.concurrent.CompletableFuture.supplyAsync(
                            () -> self.sendPushToUser(userId, title, body, data), taskExecutor))
                    .toList();

            java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0])).join();

            futures.forEach(future -> results.add(future.join()));
        }

        return results;
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

            firebaseMessaging.get().send(message);
        } catch (FirebaseMessagingException e) {
            if (isStaleTokenError(fcmErrorCode(e))) {
                deviceRepository.deleteByPushToken(token);
            }
        }
    }

    private record MulticastSendResult(int sentCount, int failedCount, List<String> staleTokens, String failureReason) {
    }

    private MulticastSendResult sendMulticastInChunks(List<String> tokens, String title, String body, Map<String, String> data) {
        if (firebaseMessaging.isEmpty() || tokens == null || tokens.isEmpty()) {
            return new MulticastSendResult(0, 0, Collections.emptyList(), null);
        }

        int sentCount = 0;
        int failedCount = 0;
        List<String> staleTokens = new ArrayList<>();
        String failureReason = null;
        Map<String, String> payload = data != null ? data : Collections.emptyMap();

        for (int start = 0; start < tokens.size(); start += FCM_MULTICAST_LIMIT) {
            int end = Math.min(start + FCM_MULTICAST_LIMIT, tokens.size());
            List<String> chunk = tokens.subList(start, end);

            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(chunk)
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
                sentCount += response.getSuccessCount();
                failedCount += response.getFailureCount();

                if (response.getFailureCount() > 0) {
                    List<SendResponse> responses = response.getResponses();
                    for (int i = 0; i < responses.size(); i++) {
                        SendResponse sendResponse = responses.get(i);
                        if (!sendResponse.isSuccessful()) {
                            String errorCode = fcmErrorCode(sendResponse.getException());
                            if (isStaleTokenError(errorCode)) {
                                staleTokens.add(chunk.get(i));
                            }
                        }
                    }
                }
            } catch (FirebaseMessagingException e) {
                failedCount += chunk.size();
                failureReason = e.getMessage();
                logger.error("Firebase messaging error during multicast chunk: {}", e.getMessage(), e);
            } catch (Exception e) {
                failedCount += chunk.size();
                failureReason = e.getMessage();
                logger.error("Unexpected error during multicast chunk: {}", e.getMessage(), e);
            }
        }

        return new MulticastSendResult(sentCount, failedCount, staleTokens, failureReason);
    }

    private static String fcmErrorCode(FirebaseMessagingException e) {
        if (e == null || e.getMessagingErrorCode() == null) {
            return "";
        }
        return e.getMessagingErrorCode().name();
    }

    private static boolean isStaleTokenError(String errorCode) {
        return "UNREGISTERED".equals(errorCode) || "INVALID_ARGUMENT".equals(errorCode);
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
