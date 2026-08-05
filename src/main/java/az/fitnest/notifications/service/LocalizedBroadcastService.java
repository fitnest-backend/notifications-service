package az.fitnest.notifications.service;

import java.util.Map;

/**
 * Fans out localized title/body variants to every user that has a current device
 * with notifications enabled.
 */
public interface LocalizedBroadcastService {

    record LocalizedContent(String title, String body) {
    }

    int broadcast(Map<String, LocalizedContent> contentsByLanguage, Map<String, String> data);
}
