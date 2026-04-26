package az.fitnest.notifications.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record BulkSendResponse(
        int total,
        int succeeded,
        int failed,
        @JsonProperty("partial_success") boolean partialSuccess,
        List<BulkSendResultItem> results
) {
}

