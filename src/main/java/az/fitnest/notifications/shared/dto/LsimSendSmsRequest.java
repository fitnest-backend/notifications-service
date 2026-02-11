package az.fitnest.notifications.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LsimSendSmsRequest {
    private String login;
    private String key;
    private String msisdn;
    private String text;
    private String sender;
    private String scheduled = "NOW";   // or "2025-03-25 14:30:00"
    private Boolean unicode = false;
}