package az.fitnest.notifications.dto;

import lombok.Builder;

@Builder
public record PushResult(
    int sentCount,
    int failedCount,
    int removedTokens,
    Long notificationId
) {}
