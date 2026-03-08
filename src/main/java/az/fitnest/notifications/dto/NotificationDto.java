package az.fitnest.notifications.dto;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record NotificationDto(
    Long id,
    String title,
    String body,
    boolean isRead,
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "dd/MM/yyyy")
    LocalDateTime createdAt
) {}
