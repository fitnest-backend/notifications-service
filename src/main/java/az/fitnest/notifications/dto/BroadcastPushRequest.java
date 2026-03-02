package az.fitnest.notifications.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record BroadcastPushRequest(
    @NotBlank(message = "Başlıq mütləqdir")
    String title,
    @NotBlank(message = "Mətn mütləqdir")
    String body
) {}
