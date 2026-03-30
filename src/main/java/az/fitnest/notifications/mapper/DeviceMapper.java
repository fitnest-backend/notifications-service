package az.fitnest.notifications.mapper;

import az.fitnest.notifications.dto.DeviceDto;
import az.fitnest.notifications.model.entity.Device;

public final class DeviceMapper {
    private DeviceMapper() {}

    public static DeviceDto toDto(Device device) {
        if (device == null) {
            return null;
        }
        return DeviceDto.builder()
                .deviceId(device.getDeviceId())
                .userId(device.getUserId())
                .pushToken(device.getPushToken())
                .createdAt(device.getCreatedAt())
                .platform(device.getPlatform())
                .notificationsEnabled(device.getNotificationEnabled())
                .isCurrent(device.getIsCurrent())
                .build();
    }
}
