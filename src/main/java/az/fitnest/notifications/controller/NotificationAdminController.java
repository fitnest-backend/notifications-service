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
    private final java.util.concurrent.Executor taskExecutor;

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

    @Operation(summary = "Xüsusi istifadəçiyə bildiriş göndərin (Admin)", description = "Seçilmiş bir istifadəçiyə push bildirişi göndərir.")
    @PostMapping("/send")
    public ResponseEntity<az.fitnest.notifications.dto.PushResult> sendPushToSingleUser(@Valid @RequestBody az.fitnest.notifications.dto.SingleUserPushRequest request) {
        return ResponseEntity.ok(notificationService.sendPushToUser(request.userId(), request.title(), request.body(), request.data()));
    }

    @Operation(summary = "Çoxlu alıcıya SMS göndərin (Admin)", description = "Verilmiş nömrələr siyahısına eyni məzmunlu SMS göndərir.")
    @PostMapping("/sms/bulk")
    public ResponseEntity<java.util.List<az.fitnest.notifications.dto.SendSmsResponse>> sendBulkSms(@Valid @RequestBody az.fitnest.notifications.dto.BulkSmsRequest request) {
        if (request.phoneNumbers() == null || request.phoneNumbers().isEmpty()) {
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }

        java.util.List<java.util.concurrent.CompletableFuture<az.fitnest.notifications.dto.SendSmsResponse>> futures = request.phoneNumbers().stream()
                .filter(phone -> phone != null && !phone.isBlank())
                .map(phone -> java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                    try {
                        Long txId = lsimSmsService.sendSms(phone, request.text());
                        return new az.fitnest.notifications.dto.SendSmsResponse(txId);
                    } catch (Exception e) {
                        return null;
                    }
                }, taskExecutor))
                .toList();

        java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0])).join();

        java.util.List<az.fitnest.notifications.dto.SendSmsResponse> responses = futures.stream()
                .map(java.util.concurrent.CompletableFuture::join)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Seçilmiş alıcılara kütləvi Email göndərin (Admin)", description = "Verilmiş email siyahısındakı bütün alıcılara eyni məzmunlu elektron poçt göndərir.")
    @PostMapping("/email/bulk")
    public ResponseEntity<Void> sendBulkEmail(@Valid @RequestBody az.fitnest.notifications.dto.BulkEmailRequest request) {
        if (request.emails() == null || request.emails().isEmpty()) {
            return ResponseEntity.ok().build();
        }

        java.util.List<java.util.concurrent.CompletableFuture<Void>> futures = request.emails().stream()
                .filter(email -> email != null && !email.isBlank())
                .map(email -> java.util.concurrent.CompletableFuture.runAsync(() -> {
                    try {
                        java.util.Map<String, Object> vars = new java.util.HashMap<>();
                        vars.put("subject", request.subject());
                        vars.put("body", request.body());
                        emailService.sendHtmlEmail(email, request.subject(), "bulk-notification.html", vars);
                    } catch (Exception e) {
                        // ignore individual email failure to continue sending others
                    }
                }, taskExecutor))
                .toList();

        java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0])).join();

        return ResponseEntity.ok().build();
    }
}
