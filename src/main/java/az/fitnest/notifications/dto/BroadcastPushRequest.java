package az.fitnest.notifications.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BroadcastPushRequest {
    
    @NotBlank(message = "Başlıq mütləqdir")
    private String title;
    
    @NotBlank(message = "Mətn mütləqdir")
    private String body;
    
    private Map<String, String> data;
}
