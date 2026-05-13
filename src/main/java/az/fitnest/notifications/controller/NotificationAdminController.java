package az.fitnest.notifications.controller;

import az.fitnest.notifications.dto.BroadcastPushRequest;
import az.fitnest.notifications.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import az.fitnest.notifications.service.EmailService;
import az.fitnest.notifications.service.LsimSmsService;

@RestController
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
@Tag(name = "Admin Notifications", description = "İstifadəçilərə yayım mesajlarını idarə etmək üçün administrativ ucluqlar")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class NotificationAdminController {

    private final NotificationService notificationService;
    private final LsimSmsService lsimSmsService;
    private final EmailService emailService;

    @Operation(summary = "Yayım bildirişi göndərin (Admin)", description = "Bütün istifadəçilərə push bildirişi göndərir. Admin rolu tələb olunur.")
    @PostMapping("/broadcast")
    public ResponseEntity<Void> broadcast(@Valid @RequestBody BroadcastPushRequest request) {
        notificationService.broadcastPushNotification(request.title(), request.body());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Seçilmiş istifadəçilərə bildiriş göndərin (Admin)", description = "Siyahıdakı xüsusi istifadəçilərə push bildirişi göndərir.")
    @PostMapping("/bulk")
    public ResponseEntity<java.util.List<az.fitnest.notifications.dto.PushResult>> sendBulkPush(@Valid @RequestBody az.fitnest.notifications.dto.BulkPushRequest request) {
        return ResponseEntity.ok(notificationService.sendPushToUsers(request.userIds(), request.title(), request.body(), request.data()));
    }

    @Operation(summary = "Çoxlu alıcıya SMS göndərin (Admin)", description = "Verilmiş nömrələr siyahısına eyni məzmunlu SMS göndərir.")
    @PostMapping("/sms/bulk")
    public ResponseEntity<java.util.List<az.fitnest.notifications.dto.SendSmsResponse>> sendBulkSms(@Valid @RequestBody az.fitnest.notifications.dto.BulkSmsRequest request) {
        java.util.List<az.fitnest.notifications.dto.SendSmsResponse> responses = new java.util.ArrayList<>();
        if (request.phoneNumbers() != null) {
            for (String phone : request.phoneNumbers()) {
                if (phone != null && !phone.isBlank()) {
                    try {
                        Long txId = lsimSmsService.sendSms(phone, request.text(), null, true, null);
                        responses.add(new az.fitnest.notifications.dto.SendSmsResponse(txId));
                    } catch (Exception e) {
                        // ignore failures for individual numbers to continue dispatching rest
                    }
                }
            }
        }
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Seçilmiş alıcılara kütləvi Email göndərin (Admin)", description = "Verilmiş email siyahısındakı bütün alıcılara eyni məzmunlu elektron poçt göndərir.")
    @PostMapping("/email/bulk")
    public ResponseEntity<Void> sendBulkEmail(@Valid @RequestBody az.fitnest.notifications.dto.BulkEmailRequest request) {
        if (request.emails() != null) {
            for (String email : request.emails()) {
                if (email != null && !email.isBlank()) {
                    emailService.sendSimpleEmail(email, request.subject(), request.body());
                }
            }
        }
        return ResponseEntity.ok().build();
    }
}
