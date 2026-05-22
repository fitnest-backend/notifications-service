package az.fitnest.notifications.service.impl;

import az.fitnest.notifications.dto.*;
import az.fitnest.notifications.exception.*;
import az.fitnest.notifications.grpc.IdentityUserGrpcClient;
import az.fitnest.notifications.repository.DeviceRepository;
import az.fitnest.notifications.service.CustomerNotificationService;
import az.fitnest.notifications.service.LsimSmsService;
import az.fitnest.notifications.service.NotificationService;
import az.fitnest.user.grpc.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
public class CustomerNotificationServiceImpl implements CustomerNotificationService {

    private final NotificationService notificationService;
    private final LsimSmsService lsimSmsService;
    private final IdentityUserGrpcClient identityUserGrpcClient;
    private final DeviceRepository deviceRepository;
    @Qualifier("taskExecutor")
    private final Executor taskExecutor;

    @Override
    public void sendSinglePush(Long customerId, AdminSendPushRequest request) {
        requireCustomer(customerId);

        List<String> tokens = deviceRepository.findPushTokensByUserId(customerId);
        if (tokens == null || tokens.isEmpty()) {
            throw new PushTokenNotFoundException();
        }

        String title = StringUtils.hasText(request.title()) ? request.title() : "Fitnest";
        try {
            notificationService.sendPushToUser(customerId, title, request.message(), mergePushData(request.data(), request.quickReplies()));
        } catch (Exception ex) {
            throw new NotificationSendFailedException(ex.getMessage());
        }
    }

    @Override
    public BulkSendResponse sendBulkPush(AdminBulkSendRequest request) {
        List<BulkSendResultItem> results = new ArrayList<>();
        int succeeded = 0;
        int failed = 0;

        List<CompletableFuture<BulkSendResultItem>> futures = request.customerIds().stream()
                .map(customerId -> CompletableFuture.supplyAsync(() -> {
                    try {
                        sendSinglePush(customerId, new AdminSendPushRequest(request.message(), request.title(), request.data(), null));
                        return BulkSendResultItem.ok(customerId);
                    } catch (BaseException ex) {
                        return BulkSendResultItem.fail(customerId, ex.getErrorCode(), ex.getMessage());
                    } catch (Exception ex) {
                        return BulkSendResultItem.fail(customerId, "NOTIFICATION_SEND_FAILED", ex.getMessage());
                    }
                }, taskExecutor))
                .toList();

        for (CompletableFuture<BulkSendResultItem> future : futures) {
            BulkSendResultItem item = future.join();
            results.add(item);
            if (item.success()) {
                succeeded++;
            } else {
                failed++;
            }
        }

        return new BulkSendResponse(results.size(), succeeded, failed, succeeded > 0 && failed > 0, results);
    }

    @Override
    public void sendSingleSms(Long customerId, AdminSendSmsRequest request) {
        UserResponse user = requireCustomer(customerId);
        String mobile = user != null ? user.getMobile() : null;
        if (!StringUtils.hasText(mobile)) {
            throw new PhoneNumberNotFoundException();
        }

        try {
            lsimSmsService.sendSms(mobile, request.message());
        } catch (Exception ex) {
            throw new NotificationSendFailedException(ex.getMessage());
        }
    }

    @Override
    public BulkSendResponse sendBulkSms(AdminBulkSendRequest request) {
        List<BulkSendResultItem> results = new ArrayList<>();
        int succeeded = 0;
        int failed = 0;

        List<CompletableFuture<BulkSendResultItem>> futures = request.customerIds().stream()
                .map(customerId -> CompletableFuture.supplyAsync(() -> {
                    try {
                        sendSingleSms(customerId, new AdminSendSmsRequest(request.message()));
                        return BulkSendResultItem.ok(customerId);
                    } catch (BaseException ex) {
                        return BulkSendResultItem.fail(customerId, ex.getErrorCode(), ex.getMessage());
                    } catch (Exception ex) {
                        return BulkSendResultItem.fail(customerId, "NOTIFICATION_SEND_FAILED", ex.getMessage());
                    }
                }, taskExecutor))
                .toList();

        for (CompletableFuture<BulkSendResultItem> future : futures) {
            BulkSendResultItem item = future.join();
            results.add(item);
            if (item.success()) {
                succeeded++;
            } else {
                failed++;
            }
        }

        return new BulkSendResponse(results.size(), succeeded, failed, succeeded > 0 && failed > 0, results);
    }

    private UserResponse requireCustomer(Long customerId) {
        try {
            UserResponse user = identityUserGrpcClient.getUserById(customerId);
            if (user == null || user.getUserId() <= 0) {
                throw new AdminCustomerNotFoundException();
            }
            return user;
        } catch (Exception ex) {
            throw new AdminCustomerNotFoundException();
        }
    }

    private Map<String, String> mergePushData(Map<String, String> data, Map<String, String> quickReplies) {
        Map<String, String> merged = new HashMap<>();
        if (data != null) merged.putAll(data);
        if (quickReplies != null) {
            quickReplies.forEach((key, value) -> {
                if (StringUtils.hasText(key) && StringUtils.hasText(value)) {
                    merged.put("quick_reply_" + key, value);
                }
            });
        }
        return merged;
    }
}

