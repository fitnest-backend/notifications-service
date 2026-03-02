package az.fitnest.notifications.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Current SMS credit balance")
public record BalanceResponse(
    @Schema(description = "Remaining SMS credits",
            example = "1500",
            requiredMode = Schema.RequiredMode.REQUIRED)
    Integer balance
) {}