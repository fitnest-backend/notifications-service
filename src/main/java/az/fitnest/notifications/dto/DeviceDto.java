package az.fitnest.notifications.dto;

import az.fitnest.notifications.model.entity.Device;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record DeviceDto(
    Long deviceId,
    Long userId,
    String pushToken,
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "dd/MM/yyyy")
    LocalDateTime createdAt,
    Device.Platform platform
) {}
