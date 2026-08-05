package az.fitnest.notifications.grpc;

import az.fitnest.user.grpc.GetActiveUsersWithLanguageRequest;
import az.fitnest.user.grpc.GetActiveUsersWithLanguageResponse;
import az.fitnest.user.grpc.UserServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class IdentityGrpcClient {

    @GrpcClient("identity-backend")
    private UserServiceGrpc.UserServiceBlockingStub userServiceBlockingStub;

    public String getUserSessionStatus(Long userId) {
        az.fitnest.user.grpc.GetUserByIdRequest request = az.fitnest.user.grpc.GetUserByIdRequest.newBuilder()
                .setUserId(userId)
                .build();
        return userServiceBlockingStub.getUserById(request).getSessionStatus();
    }

    public List<ActiveUserLanguageDto> getActiveUsersWithLanguage(List<String> roleNames) {
        List<String> roles = (roleNames == null || roleNames.isEmpty())
                ? List.of("ROLE_USER")
                : roleNames;
        GetActiveUsersWithLanguageRequest request = GetActiveUsersWithLanguageRequest.newBuilder()
                .addAllRoleNames(roles)
                .build();
        GetActiveUsersWithLanguageResponse response = userServiceBlockingStub
                .withDeadlineAfter(30, java.util.concurrent.TimeUnit.SECONDS)
                .getActiveUsersWithLanguage(request);

        if (response.getUsersCount() == 0) {
            return Collections.emptyList();
        }

        List<ActiveUserLanguageDto> result = new ArrayList<>(response.getUsersCount());
        response.getUsersList().forEach(user ->
                result.add(new ActiveUserLanguageDto(user.getUserId(), user.getLanguage())));
        return result;
    }

    public record ActiveUserLanguageDto(Long userId, String language) {
    }
}
