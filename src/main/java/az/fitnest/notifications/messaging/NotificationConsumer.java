package az.fitnest.notifications.messaging;

import az.fitnest.notifications.service.EmailService;
import az.fitnest.notifications.service.LsimSmsService;
import az.fitnest.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@Lazy(false)
@RequiredArgsConstructor
public class NotificationConsumer {

    private final EmailService emailService;
    private final LsimSmsService smsService;
    private final NotificationService notificationService;

    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("NotificationConsumer initialized and listening to 'notification-events' topic.");
    }

    @KafkaListener(topics = "notification-events", groupId = "notifications-group")
    public void consumeNotification(NotificationEvent event) {
        log.info("Received Kafka message for recipient: {}, type: {}", event.getRecipient(), event.getType());
        log.info("Consumed notification event: {}, type: {}, recipient: {}",
                event.getEventId(), event.getType(), event.getRecipient());

        try {
            switch (event.getType()) {
                case EMAIL -> handleEmail(event);
                case SMS -> handleSms(event);
                case PUSH -> handlePush(event);
                default -> log.warn("Unknown notification type: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("Failed to process notification event: {}", event.getEventId(), e);
        }
    }

    private void handleEmail(NotificationEvent event) {
        if (event.getTemplateName() != null) {
            Map<String, Object> variables = new HashMap<>();
            if (event.getVariables() != null) {
                variables.putAll(event.getVariables());
            }
            emailService.sendHtmlEmail(event.getRecipient(), event.getSubject(), event.getTemplateName(), variables);
        } else {
            emailService.sendSimpleEmail(event.getRecipient(), event.getSubject(), event.getBody());
        }
    }

    private void handleSms(NotificationEvent event) {
        smsService.sendSms(event.getRecipient(), event.getBody());
    }

    private void handlePush(NotificationEvent event) {
        if (event.getVariables() != null && event.getVariables().containsKey("userId")) {
            Long userId = Long.valueOf(event.getVariables().get("userId"));
            notificationService.sendPushToUser(userId, event.getSubject(), event.getBody(), event.getVariables());
        } else {
            log.warn("Push notification event missing userId: {}", event.getEventId());
        }
    }
}
