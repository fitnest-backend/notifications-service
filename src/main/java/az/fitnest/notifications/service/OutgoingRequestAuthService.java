package az.fitnest.notifications.service;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class OutgoingRequestAuthService {

    public String getIncomingAuthorizationHeader() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null || attributes.getRequest() == null) return null;
            return attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
        } catch (Exception ex) {
            return null;
        }
    }
}

