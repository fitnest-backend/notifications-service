package az.fitnest.notifications.controller;

import az.fitnest.notifications.dto.*;
import az.fitnest.notifications.service.CustomerNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/customers")
@RequiredArgsConstructor
@Tag(name = "Admin Customer Notifications", description = "Müştərilərə PUSH/SMS bildirişləri göndərmək üçün admin ucluqlar")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCustomerNotificationController {

    private final CustomerNotificationService customerNotificationService;

    @Operation(summary = "Single Push", description = "Tək müştəriyə PUSH bildirişi göndərir.")
    @PostMapping("/{customerId}/notifications/push")
    public ResponseEntity<Void> sendSinglePush(@PathVariable Long customerId, @Valid @RequestBody AdminSendPushRequest request) {
        customerNotificationService.sendSinglePush(customerId, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Bulk Push", description = "Bir neçə müştəriyə PUSH bildirişi göndərir. Partial success ola bilər.")
    @PostMapping("/notifications/push/bulk")
    public ResponseEntity<BulkSendResponse> sendBulkPush(@Valid @RequestBody AdminBulkSendRequest request) {
        return ResponseEntity.ok(customerNotificationService.sendBulkPush(request));
    }

    @Operation(summary = "Single SMS", description = "Tək müştəriyə SMS göndərir.")
    @PostMapping("/{customerId}/notifications/sms")
    public ResponseEntity<Void> sendSingleSms(@PathVariable Long customerId, @Valid @RequestBody AdminSendSmsRequest request) {
        customerNotificationService.sendSingleSms(customerId, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Bulk SMS", description = "Bir neçə müştəriyə SMS göndərir. Partial success ola bilər.")
    @PostMapping("/notifications/sms/bulk")
    public ResponseEntity<BulkSendResponse> sendBulkSms(@Valid @RequestBody AdminBulkSendRequest request) {
        return ResponseEntity.ok(customerNotificationService.sendBulkSms(request));
    }
}

