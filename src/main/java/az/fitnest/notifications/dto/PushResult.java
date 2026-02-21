package az.fitnest.notifications.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushResult {
    private int sentCount;
    private int failedCount;
    private int removedTokens;
    private Long notificationId;
}
