package az.fitnest.notifications.service;

import az.fitnest.notifications.dto.NotificationDto;
import az.fitnest.notifications.dto.PushResult;
import az.fitnest.notifications.model.entity.Device;
import az.fitnest.notifications.model.enums.Platform;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface NotificationService {
    void sendWelcomeSms(String phoneNumber);

    void registerDevice(Long userId, String pushToken, Platform platform);

    PushResult sendPushToUser(Long userId, String title, String body, Map<String, String> data);

    void broadcastPushNotification(String title, String body);

    void sendPushNotification(String token, String title, String body);

    void sendPushNotification(String token, String title, String body, Map<String, String> data);

    void sendToDevice(Long deviceId, String title, String body);

    Page<NotificationDto> getUserNotifications(Long userId, Pageable pageable);

    void markNotificationAsRead(Long id, Long userId);

    void markAllNotificationsAsRead(Long userId);

    void deleteNotification(Long id, Long userId);

    void deleteAllNotifications(Long userId);

    java.util.List<Device> getDevicesByUserId(Long userId);
}
