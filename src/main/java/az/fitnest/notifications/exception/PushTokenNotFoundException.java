package az.fitnest.notifications.exception;

import org.springframework.http.HttpStatus;

public class PushTokenNotFoundException extends BaseException {
    public PushTokenNotFoundException() {
        super("Push token not found", "PUSH_TOKEN_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}

