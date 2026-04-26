package az.fitnest.notifications.service;

import az.fitnest.notifications.model.enums.NotificationStatus;

public interface NotificationDeliveryLogService {
    void markResult(Long notificationId, NotificationStatus status, int sentCount, int failedCount, String failureReason);
}

