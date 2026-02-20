package az.fitnest.notifications.grpc;

import az.fitnest.notifications.service.EmailService;
import az.fitnest.notifications.service.LsimSmsService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.HashMap;
import java.util.Map;

@GrpcService
@Slf4j
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

            log.info("Sending SMS to {}: {}", to, message);

            Long transactionId = lsimSmsService.sendSms(to, message);

            log.info("SMS sent successfully, transaction ID: {}", transactionId);

            SendSMSResponse response = SendSMSResponse.newBuilder()
                    .setSuccess(true)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Failed to send SMS", e);

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

            log.info("Sending HTML email to {} with template {}", to, templateName);

            emailService.sendHtmlEmail(to, subject, templateName, new HashMap<>(variables));

            log.info("HTML email sent successfully to {}", to);

            SendEmailResponse response = SendEmailResponse.newBuilder()
                    .setSuccess(true)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Failed to send HTML email", e);

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

            log.info("Sending simple email to {}", to);

            emailService.sendSimpleEmail(to, subject, body);

            log.info("Simple email sent successfully to {}", to);

            SendEmailResponse response = SendEmailResponse.newBuilder()
                    .setSuccess(true)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Failed to send simple email", e);

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

            log.info("Sending push notification to user {}: {}", userId, title);

            int sentCount = notificationService.sendPushToUser(userId, title, body, data);

            SendPushNotificationResponse response = SendPushNotificationResponse.newBuilder()
                    .setSuccess(true)
                    .setTargetDevices(sentCount)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Failed to send push notification", e);

            SendPushNotificationResponse response = SendPushNotificationResponse.newBuilder()
                    .setSuccess(false)
                    .setErrorMessage(e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
