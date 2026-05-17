package az.fitnest.notifications.dto;

import lombok.Builder;

@Builder
public record LsimApiResponse(
    String successMessage,
    String errorMessage,
    Long obj,
    String errorCode
) {}
