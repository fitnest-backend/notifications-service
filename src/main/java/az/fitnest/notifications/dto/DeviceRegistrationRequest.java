package az.fitnest.notifications.dto;

import az.fitnest.notifications.entity.Device;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeviceRegistrationRequest {
    
    @NotBlank(message = "Push tokeni mütləqdir")
    private String pushToken;
    
    @NotNull(message = "Platforma mütləqdir")
    private Device.Platform platform;
}
