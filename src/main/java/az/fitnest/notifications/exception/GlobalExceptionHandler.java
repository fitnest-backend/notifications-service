package az.fitnest.notifications.exception;

import az.fitnest.notifications.dto.ApiError;
import az.fitnest.notifications.dto.ApiResponse;
import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException ex, HttpServletRequest request) {
        HttpStatus status = ex.getHttpStatus();
        return ResponseEntity
                .status(status)
                .body(ApiResponse.error(buildError(ex.getErrorCode(), getLocalizedMessage(ex.getErrorCode(), ex.getMessage()), status, request.getRequestURI(), null)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<Map<String, String>> fieldIssues = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "issue", error.getDefaultMessage()
                ))
                .toList();

        Map<String, Object> details = Map.of("fieldIssues", fieldIssues);
        HttpStatus status = HttpStatus.BAD_REQUEST;

        return ResponseEntity
                .status(status)
                .body(ApiResponse.error(buildError("VALIDATION_ERROR", getMessage("error.validation"), status, request.getRequestURI(), details)));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String message = getMessage("error.invalid_json_format");
        String detailText = "Invalid request body";

        Throwable cause = ex.getCause();
        if (cause instanceof JsonMappingException jme) {
            if (!jme.getPath().isEmpty()) {
                String field = jme.getPath().stream()
                        .map(JsonMappingException.Reference::getFieldName)
                        .collect(Collectors.joining("."));
                detailText = "Invalid value for field: " + field;
            } else {
                detailText = jme.getOriginalMessage();
            }
        } else if (cause != null) {
            detailText = cause.getMessage();
        }

        HttpStatus status = HttpStatus.BAD_REQUEST;
        Map<String, Object> details = Map.of("message", detailText);

        return ResponseEntity
                .status(status)
                .body(ApiResponse.error(buildError("VALIDATION_ERROR", message, status, request.getRequestURI(), details)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity
                .status(status)
                .body(ApiResponse.error(buildError("INTERNAL_SERVER_ERROR", getMessage("error.internal_server_error"), status, request.getRequestURI(), null)));
    }

    private String getLocalizedMessage(String errorCode, String defaultMessage) {
        String key = "error." + errorCode.toLowerCase();
        String message = getMessage(key);
        if (message.equals(key)) {
            // Try resolving by original errorCode
            message = getMessage(errorCode);
            if (message.equals(errorCode)) {
                return safeMessage(defaultMessage);
            }
        }
        return message;
    }

    private String safeMessage(String msg) {
        if (msg == null || msg.isBlank()) {
            return getMessage("error.unexpected");
        }
        // If the message looks like a key, try to resolve it
        if (msg.startsWith("error.")) {
            String resolved = getMessage(msg);
            if (!resolved.equals(msg)) {
                return resolved;
            }
        }
        return msg;
    }

    private String getMessage(String code) {
        try {
            return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
        } catch (Exception e) {
            return code; // Fallback to code if message not found
        }
    }

    private ApiError buildError(String code, String message, HttpStatus status, String path, Object details) {
        return ApiError.builder()
                .code(code)
                .message(message)
                .status(status != null ? status.value() : null)
                .path(path)
                .timestamp(OffsetDateTime.now())
                .details(details)
                .build();
    }
}
