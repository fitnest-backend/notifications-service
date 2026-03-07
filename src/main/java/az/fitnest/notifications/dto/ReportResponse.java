package az.fitnest.notifications.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Delivery report for a sent SMS")
public record ReportResponse(
    @Schema(description = "Transaction ID",
            example = "123456789",
            requiredMode = Schema.RequiredMode.REQUIRED)
    Long transactionId,

    @Schema(description = "Delivery status code (100-109)",
            example = "101",
            requiredMode = Schema.RequiredMode.REQUIRED)
    Integer statusCode,

    @Schema(description = "Human-readable delivery status",
            example = "Delivered",
            requiredMode = Schema.RequiredMode.REQUIRED)
    String statusDescription
) {
    public ReportResponse(Long transactionId, SmsStatus status) {
        this(transactionId, status.getCode(), status.getDescription());
    }
}
