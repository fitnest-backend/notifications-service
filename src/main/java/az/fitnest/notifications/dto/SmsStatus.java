package az.fitnest.notifications.dto;

import java.util.HashMap;
import java.util.Map;

public enum SmsStatus {
    SENT(100, "Sent"),
    ERROR(101, "Error"),
    STATUS(102, "Status"),
    EXPIRED(103, "Expired"),
    BLACK_LIST(104, "Black list"),
    IN_QUEUE(105, "In queue"),
    REJECTED(106, "Rejected"),
    DELIVERED(107, "Delivered"),
    CANCELLED(108, "Cancelled"),
    UNDELIVERED(109, "Undelivered"),
    UNKNOWN(110, "Unknown (contact us)");

    private final int code;
    private final String description;

    private static final Map<Integer, SmsStatus> CODE_MAP = new HashMap<>();

    static {
        for (SmsStatus status : SmsStatus.values()) {
            CODE_MAP.put(status.getCode(), status);
        }
    }

    SmsStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Returns the SmsStatus for the given integer code, or null if not found.
     */
    public static SmsStatus fromCode(Integer code) {
        return CODE_MAP.get(code);
    }

    /**
     * Returns the SmsStatus for the given integer code, throwing an exception if not found.
     *
     * @throws IllegalArgumentException if code is unknown
     */
    public static SmsStatus fromCodeOrThrow(Integer code) {
        SmsStatus status = CODE_MAP.get(code);
        if (status == null) {
            throw new IllegalArgumentException("Unknown SMS status code: " + code);
        }
        return status;
    }
}
