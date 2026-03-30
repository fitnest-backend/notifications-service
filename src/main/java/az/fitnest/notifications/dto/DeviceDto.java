package az.fitnest.notifications.dto;

import az.fitnest.notifications.model.entity.Device;
import az.fitnest.notifications.model.enums.Platform;
import java.time.LocalDateTime;
import lombok.Builder;

@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS)
@Builder
public record DeviceDto(
    Long deviceId,
    Long userId,
    String pushToken,
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "dd/MM/yyyy")
    LocalDateTime createdAt,
    Platform platform,
    Boolean notificationsEnabled,
    Boolean isCurrent
) {}
