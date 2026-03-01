package az.fitnest.notifications.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectPushRequest {

    @NotNull(message = "Cihaz ID mütləqdir")
    private Long deviceId;

    @NotBlank(message = "Başlıq mütləqdir")
    private String title;

    @NotBlank(message = "Mətn mütləqdir")
    private String body;
}
