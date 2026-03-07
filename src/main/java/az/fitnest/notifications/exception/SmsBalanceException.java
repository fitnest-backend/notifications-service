package az.fitnest.notifications.exception;

public class SmsBalanceException extends LsimSmsException {

    public SmsBalanceException(String message) {
        super(message);
    }

    public SmsBalanceException(String message, Integer errorCode) {
        super(message, errorCode);
    }

    public SmsBalanceException(String message, Throwable cause) {
        super(message, cause);
    }

    public SmsBalanceException(String message, Integer errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
