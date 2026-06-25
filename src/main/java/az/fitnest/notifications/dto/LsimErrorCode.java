package az.fitnest.notifications.dto;

import java.util.HashMap;
import java.util.Map;

public enum LsimErrorCode {

    INVALID_KEY(-100, "Invalid key"),
    TEXT_TOO_LONG(-101, "Text more than allowed length"),
    WRONG_NUMBER_FORMAT(-102, "Wrong number format"),
    INVALID_SENDER_NAME(-103, "Invalid sender name"),
    INSUFFICIENT_BALANCE(-104, "Insufficient balance"),
    NUMBER_IN_BLACKLIST(-105, "Number in black list"),
    INVALID_TRANSACTION_ID(-106, "Invalid transaction id"),
    IP_NOT_ALLOWED(-107, "IP address not allowed"),
    INVALID_HASH(-108, "Invalid hash"),
    NO_HOST(-109, "No host"),
    REPORTING_LIMIT_EXCEED(-110, "Reporting limit exceeded"),
    INTERNAL_ERROR(-500, "Internal error");

    private static final Map<Integer, LsimErrorCode> CODE_MAP = new HashMap<>();

    static {
        for (LsimErrorCode error : LsimErrorCode.values()) {
            CODE_MAP.put(error.getCode(), error);
        }
    }

    private final int code;
    private final String message;

    LsimErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public static LsimErrorCode fromCode(Integer code) {
        return CODE_MAP.get(code);
    }

    public static LsimErrorCode fromCodeOrThrow(Integer code) {
        LsimErrorCode error = CODE_MAP.get(code);
        if (error == null) {
            throw new IllegalArgumentException("Unknown LSIM error code: " + code);
        }
        return error;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
