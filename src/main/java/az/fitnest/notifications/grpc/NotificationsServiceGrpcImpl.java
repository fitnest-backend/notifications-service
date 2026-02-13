package az.fitnest.notifications.grpc;

import az.fitnest.notifications.service.LsimSmsService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@Slf4j
@RequiredArgsConstructor
public class NotificationsServiceGrpcImpl extends NotificationsServiceGrpc.NotificationsServiceImplBase {

    private final LsimSmsService lsimSmsService;

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
}
