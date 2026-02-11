package az.fitnest.notifications.shared.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Delivery report for a sent SMS")
public class ReportResponse {

    @Schema(description = "Transaction ID", 
            example = "123456789", 
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Long transactionId;

    @Schema(description = "Delivery status code (100-109)", 
            example = "101", 
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer statusCode;

    @Schema(description = "Human-readable delivery status", 
            example = "Delivered", 
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String statusDescription;

    public ReportResponse(Long transactionId, SmsStatus status) {
        this.transactionId = transactionId;
        this.statusCode = status.getCode();
        this.statusDescription = status.getDescription();
    }
}