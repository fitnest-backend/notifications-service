package az.fitnest.notifications.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BaseException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String errorCode) {
        super(errorCode, errorCode, HttpStatus.NOT_FOUND);
    }
}
