package az.fitnest.notifications.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "lsim.sms")
@Data
public class LsimSmsProperties {
    /**
     * When false, SMS is not sent via LSIM — {@link az.fitnest.notifications.service.LsimSmsService}
     * returns a mock success. Pair with identity {@code SMS_ENABLED=false} so OTPs become {@code 0000}.
     */
    private boolean enabled = true;
    private String baseUrl;
    private String login;
    private String password;
    private String defaultSender;
    private Boolean defaultUnicode = false;
}
