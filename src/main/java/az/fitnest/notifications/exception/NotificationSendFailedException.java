package az.fitnest.notifications.exception;

import org.springframework.http.HttpStatus;

public class NotificationSendFailedException extends BaseException {
    public NotificationSendFailedException(String message) {
        super(message != null ? message : "Notification send failed", "NOTIFICATION_SEND_FAILED", HttpStatus.BAD_GATEWAY);
    }
}

