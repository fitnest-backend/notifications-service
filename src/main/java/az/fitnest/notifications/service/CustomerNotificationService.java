package az.fitnest.notifications.service;

import az.fitnest.notifications.dto.AdminBulkSendRequest;
import az.fitnest.notifications.dto.AdminSendPushRequest;
import az.fitnest.notifications.dto.AdminSendSmsRequest;
import az.fitnest.notifications.dto.BulkSendResponse;

public interface CustomerNotificationService {
    void sendSinglePush(Long customerId, AdminSendPushRequest request);
    BulkSendResponse sendBulkPush(AdminBulkSendRequest request);

    void sendSingleSms(Long customerId, AdminSendSmsRequest request);
    BulkSendResponse sendBulkSms(AdminBulkSendRequest request);
}

