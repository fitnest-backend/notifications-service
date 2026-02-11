package az.fitnest.notifications.exceptions;

public class SmsSendException extends LsimSmsException {
    public SmsSendException(String message) {
        super(message);
    }

    public SmsSendException(String message, Integer errorCode) {
        super(message, errorCode);
    }

    public SmsSendException(String message, Throwable cause) {
        super(message, cause);
    }

    public SmsSendException(String message, Integer errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }


}