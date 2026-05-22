package az.fitnest.notifications.exception;

import org.springframework.http.HttpStatus;

public class AdminCustomerNotFoundException extends BaseException {
    public AdminCustomerNotFoundException() {
        super("Customer must exist", "CUSTOMER_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}

