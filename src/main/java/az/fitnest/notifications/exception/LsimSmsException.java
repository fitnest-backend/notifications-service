package az.fitnest.notifications.exception;


public class LsimSmsException extends RuntimeException {
    private final Integer errorCode;

    public LsimSmsException(String message) {
        super(message);
        this.errorCode = null;
    }
    public LsimSmsException(String message, Integer errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public LsimSmsException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }

    public LsimSmsException(String message, Integer errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public Integer getErrorCode() {
        return errorCode;
    }
}
