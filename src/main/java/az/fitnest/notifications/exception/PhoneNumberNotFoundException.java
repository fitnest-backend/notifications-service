package az.fitnest.notifications.exception;

import org.springframework.http.HttpStatus;

public class PhoneNumberNotFoundException extends BaseException {
    public PhoneNumberNotFoundException() {
        super("Phone number not found", "PHONE_NUMBER_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}

