package az.fitnest.notifications.dto;

import az.fitnest.notifications.model.enums.NotificationChannel;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record NotificationTemplateResponse(
        String id,
        NotificationChannel channel,
        String name,
        String title,
        String body,
        @JsonProperty("quick_replies") List<String> quickReplies
) {
}

