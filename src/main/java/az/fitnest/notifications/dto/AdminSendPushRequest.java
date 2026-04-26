package az.fitnest.notifications.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record AdminSendPushRequest(
        @NotBlank(message = "Message required")
        String message,
        String title,
        Map<String, String> data,
        @JsonProperty("quick_replies") Map<String, String> quickReplies
) {
}

