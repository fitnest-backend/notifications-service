package az.fitnest.notifications.grpc;

import az.fitnest.user.grpc.UpdateSessionStatusRequest;
import az.fitnest.user.grpc.UserServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
public class IdentityGrpcClient {

    @GrpcClient("identity-service")
    private UserServiceGrpc.UserServiceBlockingStub userServiceBlockingStub;

    public String getUserSessionStatus(Long userId) {
        az.fitnest.user.grpc.GetUserByIdRequest request = az.fitnest.user.grpc.GetUserByIdRequest.newBuilder()
                .setUserId(userId)
                .build();
        return userServiceBlockingStub.getUserById(request).getSessionStatus();
    }
}
