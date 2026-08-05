package az.fitnest.notifications.service;

import az.fitnest.notifications.model.enums.Platform;

/**
 * Owns push-device registration and lifecycle (disable on account deactivation).
 */
public interface DeviceRegistrationService {
    void registerDevice(Long userId, String pushToken, Platform platform);

    int disableAllDevicesForUser(Long userId);
}
