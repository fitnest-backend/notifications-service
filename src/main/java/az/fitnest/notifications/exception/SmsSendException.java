package az.fitnest.notifications.exception;

public class SmsSendException extends LsimSmsException {
    public SmsSendException(String message) {
        super(message);
    }

    public SmsSendException(String message, String errorCode) {
        super(message, errorCode);
    }

    public SmsSendException(String message, Throwable cause) {
        super(message, cause);
    }

    public SmsSendException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }

}
