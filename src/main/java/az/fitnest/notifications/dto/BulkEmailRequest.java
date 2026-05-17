package az.fitnest.notifications.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record BulkEmailRequest(
    @NotNull(message = "Email siyahısı boş ola bilməz")
    List<String> emails,
    
    @NotBlank(message = "Mövzu boş ola bilməz")
    String subject,
    
    @NotBlank(message = "Mesaj boş ola bilməz")
    String body
) {}
