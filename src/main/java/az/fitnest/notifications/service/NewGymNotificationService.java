package az.fitnest.notifications.service;

/**
 * Owns new-gym notification templates and triggers localized fan-out.
 */
public interface NewGymNotificationService {
    int notifyNewGym(Long gymId, String gymName);
}
