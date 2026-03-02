package az.fitnest.notifications.dto;

import lombok.Builder;

@Builder
public record LsimReportRequest(
    String login,
    Long transid
) {}
