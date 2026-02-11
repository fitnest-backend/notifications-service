package az.fitnest.notifications.controller;

import az.fitnest.notifications.service.LsimSmsService;
import az.fitnest.notifications.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    @Operation(summary = "Send an SMS message")
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
    @Operation(summary = "Check remaining SMS balance")
    public ResponseEntity<BalanceResponse> checkBalance() {
        Integer balance = lsimSmsService.checkBalance();
        return ResponseEntity.ok(new BalanceResponse(balance));
    }

    @GetMapping("/report/{transactionId}")
    @Operation(summary = "Get delivery status of a sent SMS")
    public ResponseEntity<ReportResponse> getReport(@PathVariable Long transactionId) {
        SmsStatus status = lsimSmsService.getDeliveryStatus(transactionId);
        return ResponseEntity.ok(new ReportResponse(transactionId, status));
    }
}