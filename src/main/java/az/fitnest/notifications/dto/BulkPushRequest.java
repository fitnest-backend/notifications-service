package az.fitnest.notifications.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record BulkPushRequest(
    @NotNull(message = "İstifadəçi ID-ləri mütləqdir")
    List<Long> userIds,
    String title,
    @NotBlank(message = "Mətn mütləqdir")
    String body,
    Map<String, String> data
) {}
