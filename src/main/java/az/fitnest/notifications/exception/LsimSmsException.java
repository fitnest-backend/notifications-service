package az.fitnest.notifications.exception;

import org.springframework.http.HttpStatus;

public class LsimSmsException extends BaseException {
    private final Integer errorCode;

    public LsimSmsException(String message) {
        super(message, "SMS_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        this.errorCode = null;
    }

    public LsimSmsException(String message, Integer errorCode) {
        super(message, "SMS_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        this.errorCode = errorCode;
    }

    public LsimSmsException(String message, Throwable cause) {
        super(message, "SMS_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        this.errorCode = null;
    }

    public LsimSmsException(String message, Integer errorCode, Throwable cause) {
        super(message, "SMS_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        this.errorCode = errorCode;
    }

    public Integer getErrorCode() {
        return errorCode;
    }
}
