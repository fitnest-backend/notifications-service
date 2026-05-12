package az.fitnest.notifications.exception;

import org.springframework.http.HttpStatus;

public class LsimSmsException extends BaseException {

    public LsimSmsException(String message) {
        super(message, "SMS_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public LsimSmsException(String message, String errorCode) {
        super(message, errorCode != null ? errorCode : "SMS_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public LsimSmsException(String message, Throwable cause) {
        super(message, "SMS_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public LsimSmsException(String message, String errorCode, Throwable cause) {
        super(message, errorCode != null ? errorCode : "SMS_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
