package az.fitnest.notifications.grpc;

import az.fitnest.user.grpc.GetUserByIdRequest;
import az.fitnest.user.grpc.UserResponse;
import az.fitnest.user.grpc.UserServiceGrpc;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
public class IdentityUserGrpcClient {

    @GrpcClient("identity-backend")
    private UserServiceGrpc.UserServiceBlockingStub userServiceBlockingStub;

    public UserResponse getUserById(Long userId) {
        GetUserByIdRequest request = GetUserByIdRequest.newBuilder()
                .setUserId(userId != null ? userId : 0L)
                .build();
        return userServiceBlockingStub.getUserById(request);
    }

    public boolean userExists(Long userId) {
        try {
            getUserById(userId);
            return true;
        } catch (StatusRuntimeException ex) {
            return false;
        } catch (Exception ex) {
            return false;
        }
    }
}

