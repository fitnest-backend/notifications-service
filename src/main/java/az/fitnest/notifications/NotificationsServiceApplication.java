package az.fitnest.notifications;

import az.fitnest.notifications.properties.LsimSmsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableFeignClients
@EnableJpaAuditing
@EnableConfigurationProperties(LsimSmsProperties.class)
public class NotificationsServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotificationsServiceApplication.class, args);
	}

}
