package az.fitnest.notifications.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Response after successfully sending an SMS")
public record SendSmsResponse(
    @Schema(description = "Transaction ID assigned by LSIM",
            example = "123456789",
            requiredMode = Schema.RequiredMode.REQUIRED)
    Long transactionId
) {}
