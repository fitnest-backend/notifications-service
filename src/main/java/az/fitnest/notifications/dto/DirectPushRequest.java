package az.fitnest.notifications.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record DirectPushRequest(
    @NotNull(message = "Cihaz ID mütləqdir")
    Long deviceId,
    @NotBlank(message = "Başlıq mütləqdir")
    String title,
    @NotBlank(message = "Mətn mütləqdir")
    String body
) {}
