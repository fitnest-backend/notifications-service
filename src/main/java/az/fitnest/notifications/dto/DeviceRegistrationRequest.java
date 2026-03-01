package az.fitnest.notifications.dto;

import az.fitnest.notifications.model.entity.Device;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeviceRegistrationRequest {

    @NotBlank(message = "Push tokeni mütləqdir")
    private String pushToken;
}
