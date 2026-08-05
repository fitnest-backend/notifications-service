package az.fitnest.notifications.service.impl;

import az.fitnest.notifications.exception.BadRequestException;
import az.fitnest.notifications.model.entity.Device;
import az.fitnest.notifications.model.enums.Platform;
import az.fitnest.notifications.repository.DeviceRepository;
import az.fitnest.notifications.service.DeviceRegistrationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeviceRegistrationServiceImpl implements DeviceRegistrationService {
    private static final Logger logger = LoggerFactory.getLogger(DeviceRegistrationServiceImpl.class);

    private final DeviceRepository deviceRepository;

    @Override
    @Transactional
    public void registerDevice(Long userId, String pushToken, Platform platform) {
        if (userId == null) {
            throw new BadRequestException("User id is required");
        }
        if (platform == null) {
            throw new BadRequestException("Platform must be specified and valid");
        }
        if (pushToken == null || pushToken.isBlank()) {
            throw new BadRequestException("Push token must not be blank");
        }

        final String token = pushToken.trim();

        boolean notificationEnabled = true;
        Optional<Device> previousCurrentDeviceOpt = deviceRepository.findFirstByUserIdAndIsCurrentTrue(userId);
        if (previousCurrentDeviceOpt.isPresent()) {
            notificationEnabled = Boolean.TRUE.equals(previousCurrentDeviceOpt.get().getNotificationEnabled());
        }

        try {
            upsertCurrentDevice(userId, token, platform, notificationEnabled);
        } catch (DataIntegrityViolationException first) {
            logger.warn("Device registration race for user {}, retrying upsert: {}", userId, first.getMessage());
            try {
                upsertCurrentDevice(userId, token, platform, notificationEnabled);
            } catch (DataIntegrityViolationException second) {
                logger.error("Device registration failed for user {} after retry: {}", userId, second.getMessage());
                throw new BadRequestException("Device registration failed; please retry");
            }
        }
    }

    @Override
    @Transactional
    public int disableAllDevicesForUser(Long userId) {
        if (userId == null) {
            return 0;
        }
        return deviceRepository.disableAllDevicesForUser(userId);
    }

    private void upsertCurrentDevice(Long userId, String token, Platform platform, boolean notificationEnabled) {
        Device device = resolveCanonicalDeviceByToken(token);
        Long previousOwnerId = null;

        if (device != null) {
            if (!userId.equals(device.getUserId())) {
                previousOwnerId = device.getUserId();
            }
            device.setUserId(userId);
            device.setPlatform(platform);
            device.setIsCurrent(true);
            device.setNotificationEnabled(notificationEnabled);
        } else {
            device = new Device();
            device.setUserId(userId);
            device.setPushToken(token);
            device.setPlatform(platform);
            device.setIsCurrent(true);
            device.setNotificationEnabled(notificationEnabled);
        }

        deviceRepository.saveAndFlush(device);
        deviceRepository.deactivateOtherDevices(userId, token);

        if (previousOwnerId != null) {
            restoreCurrentDeviceForUser(previousOwnerId);
        }
    }

    private Device resolveCanonicalDeviceByToken(String token) {
        List<Device> matches = deviceRepository.findAllByPushTokenOrderByNewest(token);
        if (matches.isEmpty()) {
            return null;
        }
        Device canonical = matches.get(0);
        for (int i = 1; i < matches.size(); i++) {
            deviceRepository.delete(matches.get(i));
        }
        if (matches.size() > 1) {
            deviceRepository.flush();
            logger.warn("Deduped {} duplicate device row(s) for push token {}", matches.size() - 1, maskToken(token));
        }
        return canonical;
    }

    private void restoreCurrentDeviceForUser(Long previousOwnerId) {
        if (deviceRepository.findFirstByUserIdAndIsCurrentTrue(previousOwnerId).isPresent()) {
            return;
        }
        deviceRepository.findFirstByUserIdOrderByCreatedAtDesc(previousOwnerId).ifPresent(device -> {
            device.setIsCurrent(true);
            deviceRepository.save(device);
            logger.info("Promoted device {} as current for previous owner {}", device.getDeviceId(), previousOwnerId);
        });
    }

    private static String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 4) + "***" + token.substring(token.length() - 4);
    }
}
