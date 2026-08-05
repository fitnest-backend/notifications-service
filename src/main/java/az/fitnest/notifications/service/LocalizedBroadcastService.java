package az.fitnest.notifications.service;

import java.util.List;
import java.util.Map;

/**
 * Fans out localized title/body variants to ROLE_USER (or given roles)
 * that have a current device with notifications enabled.
 */
public interface LocalizedBroadcastService {

    record LocalizedContent(String title, String body) {
    }

    int broadcast(Map<String, LocalizedContent> contentsByLanguage,
                  Map<String, String> data,
                  List<String> roleNames);
}
