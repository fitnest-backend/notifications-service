package az.fitnest.notifications.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BulkSendResultItem(
        @JsonProperty("customer_id") Long customerId,
        boolean success,
        @JsonProperty("error_code") String errorCode,
        @JsonProperty("error_message") String errorMessage
) {
    public static BulkSendResultItem ok(Long customerId) {
        return new BulkSendResultItem(customerId, true, null, null);
    }

    public static BulkSendResultItem fail(Long customerId, String errorCode, String errorMessage) {
        return new BulkSendResultItem(customerId, false, errorCode, errorMessage);
    }
}

