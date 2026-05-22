package az.fitnest.notifications.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminSendSmsRequest(
        @NotBlank(message = "Message required")
        String message
) {
}

