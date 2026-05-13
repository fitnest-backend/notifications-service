package az.fitnest.notifications.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;

@Builder
public record BulkSmsRequest(
    @NotNull(message = "Telefon nömrələri mütləqdir")
    List<String> phoneNumbers,
    @NotBlank(message = "Mətn mütləqdir")
    String text
) {}
