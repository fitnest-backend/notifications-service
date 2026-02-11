package az.fitnest.notifications.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Current SMS credit balance")
public class BalanceResponse {

    @Schema(description = "Remaining SMS credits", 
            example = "1500", 
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer balance;
}