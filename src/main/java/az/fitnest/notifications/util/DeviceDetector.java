package az.fitnest.notifications.util;

import az.fitnest.notifications.model.entity.Device;
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

        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            return null;
        }

        String ua = userAgent.toLowerCase();
        if (ua.contains("android")) {
            return Platform.ANDROID;
        } else if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ios")) {
            return Platform.IOS;
        }

        return null;
    }

    private static HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}
