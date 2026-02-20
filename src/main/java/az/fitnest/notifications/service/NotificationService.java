package az.fitnest.notifications.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {
    private final LsimSmsService lsimSmsService;
    private final az.fitnest.notifications.repository.DeviceRepository deviceRepository;

    public void sendWelcomeSms(String phoneNumber) {
        String message = "Welcome to our service!";
        Long transactionId = lsimSmsService.sendSms(phoneNumber, message);
        log.info("SMS sent, transaction ID: {}", transactionId);
    }

    public void registerDevice(Long userId, az.fitnest.notifications.dto.DeviceRegistrationRequest request) {
        deviceRepository.findByPushToken(request.getPushToken())
                .ifPresentOrElse(
                        device -> {
                            device.setUserId(userId);
                            device.setPlatform(request.getPlatform());
                            deviceRepository.save(device);
                            log.info("Updated device token for user {}", userId);
                        },
                        () -> {
                            az.fitnest.notifications.entity.Device device = new az.fitnest.notifications.entity.Device();
                            device.setUserId(userId);
                            device.setPushToken(request.getPushToken());
                            device.setPlatform(request.getPlatform());
                            device.setCreatedAt(java.time.LocalDateTime.now());
                            deviceRepository.save(device);
                            log.info("Registered new device token for user {}", userId);
                        }
                );
    }

    public int sendPushToUser(Long userId, String title, String body, java.util.Map<String, String> data) {
        java.util.List<az.fitnest.notifications.entity.Device> devices = deviceRepository.findAllByUserId(userId);
        int sentCount = 0;
        for (az.fitnest.notifications.entity.Device device : devices) {
            try {
                sendPushNotification(device.getPushToken(), title, body, data);
                sentCount++;
            } catch (Exception e) {
                log.error("Failed to send push to device {}: {}", device.getPushToken(), e.getMessage());
            }
        }
        return sentCount;
    }

    public void sendPushNotification(String token, String title, String body) {
        sendPushNotification(token, title, body, java.util.Collections.emptyMap());
    }

    public void sendPushNotification(String token, String title, String body, java.util.Map<String, String> data) {
        try {
            com.google.firebase.messaging.Message.Builder messageBuilder = com.google.firebase.messaging.Message.builder()
                    .setToken(token)
                    .setNotification(com.google.firebase.messaging.Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build());

            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            com.google.firebase.messaging.Message message = messageBuilder.build();

            String response = com.google.firebase.messaging.FirebaseMessaging.getInstance().send(message);
            log.info("Successfully sent push notification to token {}: {}", token, response);
        } catch (Exception e) {
            log.error("Error sending push notification to token {}: {}", token, e.getMessage());
            if (e.getMessage() != null && (e.getMessage().contains("registration-token-not-registered") || e.getMessage().contains("invalid-registration-token"))) {
                log.info("Removing stale token: {}", token);
                deviceRepository.deleteByPushToken(token);
            }
        }
    }
}