package az.fitnest.notifications.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Map;

public record AdminBulkSendRequest(
        @JsonProperty("customer_ids")
        @NotEmpty(message = "Bulk list cannot be empty")
        List<Long> customerIds,
        @NotBlank(message = "Message required")
        String message,
        String title,
        Map<String, String> data
) {
}

