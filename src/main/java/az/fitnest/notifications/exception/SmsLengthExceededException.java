package az.fitnest.notifications.exception;

public class SmsLengthExceededException extends LsimSmsException {

    private final int actualLength;
    private final int maxAllowed;
    private final boolean unicode;

    public SmsLengthExceededException(String message, int actualLength, int maxAllowed, boolean unicode) {
        super(message);
        this.actualLength = actualLength;
        this.maxAllowed = maxAllowed;
        this.unicode = unicode;
    }

    public int getActualLength() {
        return actualLength;
    }

    public int getMaxAllowed() {
        return maxAllowed;
    }

    public boolean isUnicode() {
        return unicode;
    }
}
