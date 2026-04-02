package az.fitnest.notifications.util;

import az.fitnest.notifications.model.enums.Platform;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class DeviceDetector {

    public static Platform detectPlatform() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return null;
        }

        String platformHeader = request.getHeader("X-Platform");
        if (platformHeader != null && !platformHeader.trim().isEmpty()) {
            if ("IOS".equalsIgnoreCase(platformHeader.trim())) {
                return Platform.IOS;
            } else if ("ANDROID".equalsIgnoreCase(platformHeader.trim())) {
                return Platform.ANDROID;
            }
        }

        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.trim().isEmpty()) {
            return null;
        }

        String ua = userAgent.toLowerCase();

        if (ua.contains("android") || ua.contains("dalvik")) {
            return Platform.ANDROID;
        }

        if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ipod")
            || ua.contains("ios") || ua.contains("cfnetwork") || ua.contains("darwin")) {
            return Platform.IOS;
        }

        return null;
    }

    private static HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}
