package az.fitnest.notifications.exception;

import org.springframework.http.HttpStatus;

public class InvalidDateRangeException extends BaseException {
    public InvalidDateRangeException(String message) {
        super(message != null ? message : "Date filters must be valid", "INVALID_DATE_RANGE", HttpStatus.BAD_REQUEST);
    }
}

