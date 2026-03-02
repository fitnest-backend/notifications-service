package az.fitnest.notifications.grpc;

import az.fitnest.notifications.service.EmailService;
import az.fitnest.notifications.service.LsimSmsService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.HashMap;
import java.util.Map;

@GrpcService
@RequiredArgsConstructor
public class NotificationsServiceGrpcImpl extends NotificationsServiceGrpc.NotificationsServiceImplBase {

    private final LsimSmsService lsimSmsService;
    private final EmailService emailService;
    private final az.fitnest.notifications.service.NotificationService notificationService;

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
}
