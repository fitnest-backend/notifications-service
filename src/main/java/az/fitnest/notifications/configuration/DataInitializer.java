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

            // Add notifications for admin user with ID 1
            Notification adminNotification = new Notification();
            adminNotification.setUserId(1L);
            adminNotification.setTitle("Welcome Admin!");
            adminNotification.setBody("Welcome to the Fitnest Administration Panel.");
            adminNotification.setRead(false);
            adminNotification.setStatus(NotificationStatus.SENT);
            adminNotification.setSentCount(1);
            notificationRepository.save(adminNotification);

            Notification adminSystemAlert = new Notification();
            adminSystemAlert.setUserId(1L);
            adminSystemAlert.setTitle("System Alert");
            adminSystemAlert.setBody("All systems are operational.");
            adminSystemAlert.setRead(false);
            adminSystemAlert.setStatus(NotificationStatus.SENT);
            adminSystemAlert.setSentCount(1);
            notificationRepository.save(adminSystemAlert);
        }
    }
}
