package az.fitnest.notifications.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.Map;

@Builder
public record SingleUserPushRequest(
    @NotNull(message = "İstifadəçi ID mütləqdir")
    Long userId,
    @NotBlank(message = "Başlıq mütləqdir")
    String title,
    @NotBlank(message = "Mətn mütləqdir")
    String body,
    Map<String, String> data
) {}

