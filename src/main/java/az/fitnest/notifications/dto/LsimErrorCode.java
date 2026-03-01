package az.fitnest.notifications.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * Error codes returned by LSIM Quick SMS API.
 * See: Error responses table in documentation.
 */
public enum LsimErrorCode {

    NO_HOST(-100, "no host"),
    INVALID_KEY(-101, "invalid key"),
    INVALID_HASH(-102, "invalid hash"),
    INTERNAL_ERROR(-103, "internal error"),
    INSUFFICIENT_BALANCE(-104, "insufficient balance"),
    NUMBER_IN_BLACKLIST(-105, "number in black list"),
    INVALID_SENDER_NAME(-106, "invalid sender name"),
    INVALID_TRANSACTION_ID(-107, "invalid transaction id"),
    WRONG_NUMBER_FORMAT(-108, "wrong number format"),
    REPORTING_LIMIT_EXCEED(-109, "reporting limit exceed"),
    IP_NOT_ALLOWED(-110, "IP address not allowed"),
    TEXT_TOO_LONG(-500, "text more than allowed length");

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

    /**
     * Returns the LsimErrorCode for the given integer code, or null if not found.
     */
    public static LsimErrorCode fromCode(Integer code) {
        return CODE_MAP.get(code);
    }

    /**
     * Returns the LsimErrorCode for the given integer code, or throws if not found.
     */
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