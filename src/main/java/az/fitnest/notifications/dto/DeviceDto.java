package az.fitnest.notifications.dto;

import az.fitnest.notifications.entity.Device;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceDto {
    private Long deviceId;
    private Long userId;
    private String pushToken;
    private Device.Platform platform;
    private LocalDateTime createdAt;
}
