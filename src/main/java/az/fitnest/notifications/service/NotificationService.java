package az.fitnest.notifications.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {
    private final LsimSmsService lsimSmsService;

    public void sendWelcomeSms(String phoneNumber) {
        String message = "Welcome to our service!";
        Long transactionId = lsimSmsService.sendSms(phoneNumber, message);
        log.info("SMS sent, transaction ID: {}", transactionId);
    }
}