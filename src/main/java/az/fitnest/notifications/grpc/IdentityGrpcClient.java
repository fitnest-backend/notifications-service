package az.fitnest.notifications.grpc;

import az.fitnest.user.grpc.GetUsersByIdsRequest;
import az.fitnest.user.grpc.GetUsersByIdsResponse;
import az.fitnest.user.grpc.UserResponse;
import az.fitnest.user.grpc.UserServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class IdentityGrpcClient {
    private static final Logger logger = LoggerFactory.getLogger(IdentityGrpcClient.class);
    private static final int USER_LOOKUP_BATCH_SIZE = 100;

    @GrpcClient("identity-backend")
    private UserServiceGrpc.UserServiceBlockingStub userServiceBlockingStub;

    public String getUserSessionStatus(Long userId) {
        az.fitnest.user.grpc.GetUserByIdRequest request = az.fitnest.user.grpc.GetUserByIdRequest.newBuilder()
                .setUserId(userId)
                .build();
        return userServiceBlockingStub
                .withDeadlineAfter(5, TimeUnit.SECONDS)
                .getUserById(request)
                .getSessionStatus();
    }

    /**
     * Batch-loads preferred language for the given user ids.
     * On partial/total failure, missing users default to language AZ.
     */
    public Map<Long, UserLanguageDto> getUsersLanguageByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, UserLanguageDto> result = new HashMap<>();
        for (Long userId : userIds) {
            result.put(userId, new UserLanguageDto(userId, "AZ"));
        }

        for (int start = 0; start < userIds.size(); start += USER_LOOKUP_BATCH_SIZE) {
            int end = Math.min(start + USER_LOOKUP_BATCH_SIZE, userIds.size());
            List<Long> batch = userIds.subList(start, end);
            try {
                GetUsersByIdsResponse response = userServiceBlockingStub
                        .withDeadlineAfter(10, TimeUnit.SECONDS)
                        .getUsersByIds(GetUsersByIdsRequest.newBuilder()
                                .addAllUserIds(batch)
                                .build());
                for (UserResponse user : response.getUsersList()) {
                    result.put(user.getUserId(), new UserLanguageDto(user.getUserId(), user.getLanguage()));
                }
            } catch (Exception e) {
                logger.warn("Failed to load user languages for batch starting at {}: {}. Falling back to AZ.",
                        start, e.getMessage());
            }
        }
        return result;
    }

    public record UserLanguageDto(Long userId, String language) {
    }
}
