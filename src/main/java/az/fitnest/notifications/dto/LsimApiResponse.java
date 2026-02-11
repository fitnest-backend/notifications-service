package az.fitnest.notifications.dto;

import lombok.Data;

@Data
public class LsimApiResponse {
    private String successMessage;
    private String errorMessage;
    private Long obj;          // transaction ID (for send) or balance (for balance API)
    private Integer errorCode;
}