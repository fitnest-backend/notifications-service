package az.fitnest.notifications.service;

import az.fitnest.notifications.model.entity.Device;
import az.fitnest.notifications.repository.DeviceRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DevicePlatformService {
    private final DeviceRepository deviceRepository;

    public DevicePlatformService(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    public String getUserPlatform(Long userId) {
        List<Device> devices = deviceRepository.findAllByUserId(userId);
        if (devices.isEmpty()) return "UNKNOWN";
        if (devices.size() == 1) return devices.get(0).getPlatform().name();
        Device latest = devices.stream().max((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt())).orElse(devices.get(0));
        return latest.getPlatform().name();
    }
}
