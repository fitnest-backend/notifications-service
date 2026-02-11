package az.fitnest.notifications.dto;

import java.util.HashMap;
import java.util.Map;

public enum SmsStatus {
    IN_QUEUE(100, "In queue"),
    DELIVERED(101, "Delivered"),
    UNDELIVERED(102, "Undelivered"),
    EXPIRED(103, "Expired"),
    REJECTED(104, "Rejected"),
    CANCELLED(105, "Cancelled"),
    ERROR(106, "Error"),
    UNKNOWN(107, "Unknown (contact us)"),
    SENT(108, "Sent"),
    BLACK_LIST(109, "Black list");

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
