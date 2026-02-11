package az.fitnest.notifications.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "lsim.sms")
@Component
@Data
public class LsimSmsProperties {
    private String baseUrl;
    private String login;
    private String password;
    private String defaultSender;
    private Boolean defaultUnicode = false;
}