package az.fitnest.notifications.service;

import az.fitnest.notifications.dto.NotificationDto;
import az.fitnest.notifications.dto.PushResult;
import az.fitnest.notifications.entity.Device;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Map;

public interface NotificationService {
    void sendWelcomeSms(String phoneNumber);
    void registerDevice(Long userId, String pushToken, Device.Platform platform);
    PushResult sendPushToUser(Long userId, String title, String body, Map<String, String> data);
    void broadcastPushNotification(String title, String body);
    void sendPushNotification(String token, String title, String body);
    void sendPushNotification(String token, String title, String body, Map<String, String> data);
    void sendToUser(Long userId, String title, String body);
    Page<NotificationDto> getUserNotifications(Long userId, Pageable pageable);
}
