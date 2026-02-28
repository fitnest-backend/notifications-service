package az.fitnest.notifications.configuration;
import az.fitnest.notifications.model.enums.NotificationStatus;

import az.fitnest.notifications.model.entity.Notification;
import az.fitnest.notifications.model.enums.NotificationStatus;
import az.fitnest.notifications.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final NotificationRepository notificationRepository;

    @Bean
    public CommandLineRunner initNotificationData() {
        return args -> {
            initNotifications();
        };
    }

    private void initNotifications() {
        if (notificationRepository.count() == 0) {
            Notification notification = new Notification();
            notification.setUserId(2L); // Sample Super Admin ID
            notification.setTitle("Welcome to Fitnest!");
            notification.setBody("We're glad to have you here. Explore our features and stay fit!");
            notification.setRead(false);
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentCount(1);
            
            notificationRepository.save(notification);

            Notification updateNotification = new Notification();
            updateNotification.setUserId(2L);
            updateNotification.setTitle("New Update Available");
            updateNotification.setBody("A new version of the app is available. Check out the latest features.");
            updateNotification.setRead(true);
            updateNotification.setStatus(NotificationStatus.SENT);
            updateNotification.setSentCount(1);

            notificationRepository.save(updateNotification);
        }
    }
}
