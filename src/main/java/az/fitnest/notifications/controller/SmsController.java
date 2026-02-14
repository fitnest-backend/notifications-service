package az.fitnest.notifications.controller;

import az.fitnest.notifications.service.LsimSmsService;
import az.fitnest.notifications.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sms")
@Tag(name = "SMS Service", description = "Send SMS, check balance, get delivery reports")
@RequiredArgsConstructor
public class SmsController {

    private final LsimSmsService lsimSmsService;

    @PostMapping("/send")
    @Operation(
            summary = "Send an SMS message",
            description = "Sends an SMS message to the specified recipient. The message can be scheduled for future delivery or sent immediately. Supports Unicode for international characters and emojis. Returns a transaction ID for tracking delivery status."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "SMS sent successfully",
                    content = @Content(schema = @Schema(implementation = SendSmsResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data or validation failed",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error or SMS provider failure",
                    content = @Content
            )
    })
    public ResponseEntity<SendSmsResponse> sendSms(@Valid @RequestBody SendSmsRequest request) {
        Long transactionId = lsimSmsService.sendSms(
                request.getMsisdn(),
                request.getText(),
                request.getSender(),
                request.getUnicode(),
                request.getScheduled()
        );
        return ResponseEntity.ok(new SendSmsResponse(transactionId));
    }

    @GetMapping("/balance")
    @Operation(
            summary = "Check remaining SMS balance",
            description = "Retrieves the current number of remaining SMS credits available for sending messages. This balance is managed by the LSIM SMS provider and is deducted based on message length and type."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Balance retrieved successfully",
                    content = @Content(schema = @Schema(implementation = BalanceResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error or provider communication failure",
                    content = @Content
            )
    })
    public ResponseEntity<BalanceResponse> checkBalance() {
        Integer balance = lsimSmsService.checkBalance();
        return ResponseEntity.ok(new BalanceResponse(balance));
    }

    @GetMapping("/report/{transactionId}")
    @Operation(
            summary = "Get delivery status of a sent SMS",
            description = "Retrieves the delivery status of a previously sent SMS using its transaction ID. Status codes indicate whether the message was delivered, failed, or is still in progress."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Delivery status retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ReportResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Transaction ID not found",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error or provider communication failure",
                    content = @Content
            )
    })
    public ResponseEntity<ReportResponse> getReport(@PathVariable Long transactionId) {
        SmsStatus status = lsimSmsService.getDeliveryStatus(transactionId);
        return ResponseEntity.ok(new ReportResponse(transactionId, status));
    }
}