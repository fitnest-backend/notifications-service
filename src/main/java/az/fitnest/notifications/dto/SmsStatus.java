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
    UNKNOWN(107, "Unknown"),
    SENT(108, "Sent"),
    BLACK_LIST(109, "Black list");

    private static final Map<Integer, SmsStatus> CODE_MAP = new HashMap<>();

    static {
        for (SmsStatus status : SmsStatus.values()) {
            CODE_MAP.put(status.getCode(), status);
        }
    }

    private final int code;
    private final String description;

    SmsStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static SmsStatus fromCode(Integer code) {
        return CODE_MAP.get(code);
    }

    public static SmsStatus fromCodeOrThrow(Integer code) {
        SmsStatus status = CODE_MAP.get(code);
        if (status == null) {
            throw new IllegalArgumentException("Unknown SMS status code: " + code);
        }
        return status;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
