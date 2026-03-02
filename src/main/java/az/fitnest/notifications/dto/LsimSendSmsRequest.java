package az.fitnest.notifications.dto;

import lombok.Builder;

@Builder
public record LsimSendSmsRequest(
    String login,
    String key,
    String msisdn,
    String text,
    String sender,
    String scheduled,
    Boolean unicode
) {}