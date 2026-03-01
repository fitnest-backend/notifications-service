package az.fitnest.notifications.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response after successfully sending an SMS")
public class SendSmsResponse {

    @Schema(description = "Transaction ID assigned by LSIM",
            example = "123456789",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Long transactionId;
}