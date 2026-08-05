package az.fitnest.notifications.grpc;

import az.fitnest.notifications.service.EmailService;
import az.fitnest.notifications.service.LsimSmsService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@GrpcService
@RequiredArgsConstructor
public class NotificationsServiceGrpcImpl extends NotificationsServiceGrpc.NotificationsServiceImplBase {
    private static final Logger logger = LoggerFactory.getLogger(NotificationsServiceGrpcImpl.class);

    private final LsimSmsService lsimSmsService;
    private final EmailService emailService;
    private final az.fitnest.notifications.service.NotificationService notificationService;
    private final az.fitnest.notifications.service.NewGymNotificationService newGymNotificationService;
    private final az.fitnest.notifications.service.LocalizedBroadcastService localizedBroadcastService;

    @Override
    public void sendSMS(SendSMSRequest request, StreamObserver<SendSMSResponse> responseObserver) {
        try {
            String to = request.getTo();
            String message = request.getMessage();

            Long transactionId = lsimSmsService.sendSms(to, message);

            SendSMSResponse response = SendSMSResponse.newBuilder()
                    .setSuccess(true)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {

            SendSMSResponse response = SendSMSResponse.newBuilder()
                    .setSuccess(false)
                    .setErrorMessage(e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void sendHtmlEmail(SendHtmlEmailRequest request, StreamObserver<SendEmailResponse> responseObserver) {
        try {
            String to = request.getTo();
            String subject = request.getSubject();
            String templateName = request.getTemplateName();
            Map<String, String> variables = request.getVariablesMap();

            emailService.sendHtmlEmail(to, subject, templateName, new HashMap<>(variables));

            SendEmailResponse response = SendEmailResponse.newBuilder()
                    .setSuccess(true)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {

            SendEmailResponse response = SendEmailResponse.newBuilder()
                    .setSuccess(false)
                    .setErrorMessage(e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void sendSimpleEmail(SendSimpleEmailRequest request, StreamObserver<SendEmailResponse> responseObserver) {
        try {
            String to = request.getTo();
            String subject = request.getSubject();
            String body = request.getBody();

            emailService.sendSimpleEmail(to, subject, body);

            SendEmailResponse response = SendEmailResponse.newBuilder()
                    .setSuccess(true)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {

            SendEmailResponse response = SendEmailResponse.newBuilder()
                    .setSuccess(false)
                    .setErrorMessage(e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void sendPushNotification(SendPushNotificationRequest request, StreamObserver<SendPushNotificationResponse> responseObserver) {
        try {
            Long userId = request.getUserId();
            String title = request.getTitle();
            String body = request.getBody();
            Map<String, String> data = request.getDataMap();

            az.fitnest.notifications.dto.PushResult result = notificationService.sendPushToUser(userId, title, body, data);

            SendPushNotificationResponse response = SendPushNotificationResponse.newBuilder()
                    .setSuccess(true)
                    .setTargetDevices(result.sentCount())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {

            SendPushNotificationResponse response = SendPushNotificationResponse.newBuilder()
                    .setSuccess(false)
                    .setErrorMessage(e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void broadcastLocalizedPushNotification(BroadcastLocalizedPushRequest request,
                                                   StreamObserver<BroadcastLocalizedPushResponse> responseObserver) {
        try {
            java.util.Map<String, az.fitnest.notifications.service.LocalizedBroadcastService.LocalizedContent> contents =
                    new HashMap<>();
            for (LocalizedPushContent content : request.getContentsList()) {
                if (content.getLanguage() == null || content.getLanguage().isBlank()) {
                    continue;
                }
                contents.put(content.getLanguage(),
                        new az.fitnest.notifications.service.LocalizedBroadcastService.LocalizedContent(
                                content.getTitle(), content.getBody()));
            }

            int targetUsers = localizedBroadcastService.broadcast(
                    contents,
                    request.getDataMap(),
                    request.getRoleNamesList());

            BroadcastLocalizedPushResponse response = BroadcastLocalizedPushResponse.newBuilder()
                    .setSuccess(true)
                    .setTargetUsers(targetUsers)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Localized broadcast failed: {}", e.getMessage(), e);
            BroadcastLocalizedPushResponse response = BroadcastLocalizedPushResponse.newBuilder()
                    .setSuccess(false)
                    .setErrorMessage(e.getMessage() != null ? e.getMessage() : "Unknown error")
                    .setTargetUsers(0)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void notifyNewGym(NotifyNewGymRequest request, StreamObserver<NotifyNewGymResponse> responseObserver) {
        try {
            int targetUsers = newGymNotificationService.notifyNewGym(request.getGymId(), request.getGymName());
            NotifyNewGymResponse response = NotifyNewGymResponse.newBuilder()
                    .setSuccess(true)
                    .setTargetUsers(targetUsers)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("NotifyNewGym failed for gymId={}: {}", request.getGymId(), e.getMessage(), e);
            NotifyNewGymResponse response = NotifyNewGymResponse.newBuilder()
                    .setSuccess(false)
                    .setErrorMessage(e.getMessage() != null ? e.getMessage() : "Unknown error")
                    .setTargetUsers(0)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getNotifications(GetNotificationsRequest request, StreamObserver<GetNotificationsResponse> responseObserver) {
        try {
            Long userId = request.getUserId();
            int page = request.getPage() >= 0 ? request.getPage() : 0;
            int size = request.getSize() > 0 ? request.getSize() : 20;

            org.springframework.data.domain.Page<az.fitnest.notifications.dto.NotificationDto> notificationPage =
                    notificationService.getUserNotifications(userId, org.springframework.data.domain.PageRequest.of(page, size));

            java.util.List<NotificationDto> notifications = notificationPage.getContent().stream()
                    .map(dto -> NotificationDto.newBuilder()
                            .setId(dto.id())
                            .setTitle(dto.title())
                            .setBody(dto.body())
                            .setIsRead(dto.isRead())
                            .setCreatedAt(dto.createdAt() != null ? dto.createdAt().toString() : "")
                            .build())
                    .collect(java.util.stream.Collectors.toList());

            GetNotificationsResponse response = GetNotificationsResponse.newBuilder()
                    .addAllNotifications(notifications)
                    .setCurrentPage(notificationPage.getNumber())
                    .setTotalPages(notificationPage.getTotalPages())
                    .setTotalElements(notificationPage.getTotalElements())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException());
        }
    }

    @Override
    public void setUserNotificationPreference(SetUserNotificationPreferenceRequest request, StreamObserver<SetUserNotificationPreferenceResponse> responseObserver) {
        try {
            long userId = request.getUserId();
            boolean enabled = request.getNotificationsEnabled();
            java.util.List<az.fitnest.notifications.model.entity.Device> devices = notificationService.getDevicesByUserId(userId);
            logger.info("[setUserNotificationPreference] userId={}, requested notificationsEnabled={}, devices={}", userId, enabled, devices.stream().map(d -> String.format("{id=%d, isCurrent=%s, notificationsEnabled=%s}", d.getDeviceId(), d.getIsCurrent(), d.getNotificationEnabled())).toList());
            boolean updated = false;
            for (az.fitnest.notifications.model.entity.Device device : devices) {
                if (Boolean.TRUE.equals(device.getIsCurrent())) {
                    logger.info("[setUserNotificationPreference] Updating deviceId={} (was notificationsEnabled={})", device.getDeviceId(), device.getNotificationEnabled());
                    device.setNotificationEnabled(enabled);
                    notificationService.saveDevice(device);
                    logger.info("[setUserNotificationPreference] Updated deviceId={} (now notificationsEnabled={})", device.getDeviceId(), device.getNotificationEnabled());
                    updated = true;
                }
            }
            SetUserNotificationPreferenceResponse response = SetUserNotificationPreferenceResponse.newBuilder()
                    .setSuccess(updated)
                    .setErrorMessage(updated ? "" : "No current device found for user")
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            SetUserNotificationPreferenceResponse response = SetUserNotificationPreferenceResponse.newBuilder()
                    .setSuccess(false)
                    .setErrorMessage(e.getMessage())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getDevicesByUserId(GetDevicesByUserIdRequest request, StreamObserver<GetDevicesByUserIdResponse> responseObserver) {
        try {
            Long userId = request.getUserId();
            java.util.List<az.fitnest.notifications.model.entity.Device> devices = notificationService.getDevicesByUserId(userId);
            java.util.List<Device> grpcDevices = devices.stream()
                .map(device -> Device.newBuilder()
                    .setDeviceId(device.getDeviceId() != null ? device.getDeviceId() : 0)
                    .setUserId(device.getUserId() != null ? device.getUserId() : 0)
                    .setPushToken(device.getPushToken() != null ? device.getPushToken() : "")
                    .setPlatform(device.getPlatform() != null ? device.getPlatform().name() : "")
                    .setCreatedAt(device.getCreatedAt() != null ? device.getCreatedAt().toString() : "")
                    .setNotificationsEnabled(device.getNotificationEnabled() != null ? device.getNotificationEnabled() : false)
                    .setIsCurrent(device.getIsCurrent() != null ? device.getIsCurrent() : false)
                    .build())
                .collect(java.util.stream.Collectors.toList());
            GetDevicesByUserIdResponse response = GetDevicesByUserIdResponse.newBuilder()
                .addAllDevices(grpcDevices)
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException());
        }
    }
}
