package az.fitnest.notifications.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
@Schema(description = "Request payload to send an SMS")
public record SendSmsRequest(
    @Schema(example = "994501234567", description = "Recipient phone number with country code")
    @NotBlank
    String msisdn,

    @Schema(example = "Hello world!", description = "SMS text content")
    @NotBlank
    @Size(max = 1377)
    String text,

    @Schema(example = "MyBrand", description = "Sender title (provided by LSIM)")
    String sender,

    @Schema(example = "false", description = "Use true for Unicode (e.g., Cyrillic, emoji)")
    Boolean unicode,

    @Schema(example = "NOW", description = "Schedule time (NOW or YYYY-MM-DD HH:mm:ss)")
    String scheduled
) {}
