package az.fitnest.notifications.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends BaseException {

    private static final long serialVersionUID = 1L;

    public BadRequestException(String errorCode) {
        super(errorCode, errorCode, HttpStatus.BAD_REQUEST);
    }
}
