package az.fitnest.notifications.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record DeviceRegistrationRequest(
    @NotBlank(message = "Push tokeni mütləqdir")
    String pushToken
) {}
