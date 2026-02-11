package az.fitnest.notifications.exception;

public class SmsReportException extends LsimSmsException {

    public SmsReportException(String message) {
        super(message);
    }

    public SmsReportException(String message, Integer errorCode) {
        super(message, errorCode);
    }

    public SmsReportException(String message, Throwable cause) {
        super(message, cause);
    }

    public SmsReportException(String message, Integer errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}