package az.fitnest.notifications.service.impl;

import az.fitnest.notifications.grpc.IdentityGrpcClient;
import az.fitnest.notifications.repository.DeviceRepository;
import az.fitnest.notifications.service.LocalizedBroadcastService;
import az.fitnest.notifications.service.PushDeliveryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LocalizedBroadcastServiceImpl implements LocalizedBroadcastService {
    private static final Logger logger = LoggerFactory.getLogger(LocalizedBroadcastServiceImpl.class);

    private final IdentityGrpcClient identityGrpcClient;
    private final DeviceRepository deviceRepository;
    private final PushDeliveryService pushDeliveryService;

    @Override
    public int broadcast(Map<String, LocalizedContent> contentsByLanguage,
                         Map<String, String> data,
                         List<String> roleNames) {
        if (contentsByLanguage == null || contentsByLanguage.isEmpty()) {
            logger.warn("Localized broadcast skipped: no contents provided");
            return 0;
        }

        Map<String, LocalizedContent> normalizedContents = new HashMap<>();
        contentsByLanguage.forEach((lang, content) -> {
            if (lang != null && content != null) {
                normalizedContents.put(lang.trim().toUpperCase(Locale.ROOT), content);
            }
        });

        LocalizedContent fallback = normalizedContents.getOrDefault("AZ",
                normalizedContents.values().iterator().next());

        // Device-first: only users who can actually receive push
        List<Long> pushEligibleUserIds = deviceRepository.findUserIdsWithActivePushEnabled();
        if (pushEligibleUserIds.isEmpty()) {
            logger.info("Localized broadcast skipped: no users with active push-enabled devices");
            return 0;
        }

        Set<String> allowedRoles = normalizeRoles(roleNames);
        Map<Long, IdentityGrpcClient.UserLanguageDto> userMeta =
                identityGrpcClient.getUsersLanguageByIds(pushEligibleUserIds);

        Map<String, List<Long>> userIdsByLanguage = new HashMap<>();
        for (Long userId : pushEligibleUserIds) {
            IdentityGrpcClient.UserLanguageDto meta = userMeta.get(userId);
            if (meta != null && !allowedRoles.isEmpty() && !isAllowedRole(meta.role(), allowedRoles)) {
                continue;
            }
            String lang = normalizeLanguage(meta != null ? meta.language() : null);
            userIdsByLanguage.computeIfAbsent(lang, key -> new ArrayList<>()).add(userId);
        }

        if (userIdsByLanguage.isEmpty()) {
            logger.info("Localized broadcast skipped: no eligible users after role filter");
            return 0;
        }

        Map<String, String> payload = data != null ? data : Collections.emptyMap();
        int targetUsers = 0;

        for (Map.Entry<String, List<Long>> entry : userIdsByLanguage.entrySet()) {
            LocalizedContent content = normalizedContents.getOrDefault(entry.getKey(), fallback);
            pushDeliveryService.deliverToUsersIgnoringSession(
                    entry.getValue(), content.title(), content.body(), payload);
            targetUsers += entry.getValue().size();
        }

        logger.info("Localized broadcast completed for {} push-eligible users across {} language groups",
                targetUsers, userIdsByLanguage.size());
        return targetUsers;
    }

    private static Set<String> normalizeRoles(List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return Set.of("ROLE_USER");
        }
        Set<String> roles = new HashSet<>();
        for (String role : roleNames) {
            if (role != null && !role.isBlank()) {
                roles.add(role.trim().toUpperCase(Locale.ROOT));
            }
        }
        return roles.isEmpty() ? Set.of("ROLE_USER") : roles;
    }

    private static boolean isAllowedRole(String role, Set<String> allowedRoles) {
        if (role == null || role.isBlank()) {
            // Identity may omit role on older payloads — keep device-registered users
            return true;
        }
        return allowedRoles.contains(role.trim().toUpperCase(Locale.ROOT));
    }

    private static String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return "AZ";
        }
        String normalized = language.trim().toUpperCase(Locale.ROOT);
        if ("EN".equals(normalized) || "RU".equals(normalized) || "AZ".equals(normalized)) {
            return normalized;
        }
        return "AZ";
    }
}
