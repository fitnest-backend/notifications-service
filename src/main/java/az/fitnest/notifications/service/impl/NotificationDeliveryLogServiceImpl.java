package az.fitnest.notifications.service.impl;

import az.fitnest.notifications.model.enums.NotificationStatus;
import az.fitnest.notifications.repository.NotificationRepository;
import az.fitnest.notifications.service.NotificationDeliveryLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationDeliveryLogServiceImpl implements NotificationDeliveryLogService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public void markResult(Long notificationId, NotificationStatus status, int sentCount, int failedCount, String failureReason) {
        if (notificationId == null) return;
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setStatus(status);
            notification.setSentCount(sentCount);
            notification.setFailedCount(failedCount);
            notification.setFailureReason(failureReason);
        });
    }
}

