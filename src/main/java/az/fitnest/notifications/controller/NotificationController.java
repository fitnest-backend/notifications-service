package az.fitnest.notifications.controller;

import az.fitnest.notifications.dto.BroadcastPushRequest;
import az.fitnest.notifications.dto.NotificationDto;
import az.fitnest.notifications.dto.PaginatedResponse;
import az.fitnest.notifications.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "İstifadəçi bildirişlərini və yayım mesajlarını idarə etmək üçün ucluqlar")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "İstifadəçi bildirişlərini əldə edin", description = "Autentifikasiya olunmuş istifadəçi üçün bildirişlərin səhifələnmiş siyahısını qaytarır.")
    @GetMapping
    public ResponseEntity<PaginatedResponse<NotificationDto>> getUserNotifications(
            @AuthenticationPrincipal Object principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = extractUserId(principal);
        return ResponseEntity.ok(PaginatedResponse.of(notificationService.getUserNotifications(userId, PageRequest.of(page, size))));
    }

    @Operation(summary = "Bildirişi oxunmuş kimi qeyd edin", description = "Verilmiş bildirişi oxunmuş kimi qeyd edir.")
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long id) {
        Long userId = extractUserId(principal);
        if (userId != null) {
            notificationService.markNotificationAsRead(id, userId);
        }
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Bütün bildirişləri oxunmuş kimi qeyd edin", description = "İstifadəçinin bütün bildirişlərini oxunmuş kimi qeyd edir.")
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal Object principal) {
        Long userId = extractUserId(principal);
        if (userId != null) {
            notificationService.markAllNotificationsAsRead(userId);
        }
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Bildirişi silin", description = "Verilmiş bildirişi silir.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long id) {
        Long userId = extractUserId(principal);
        if (userId != null) {
            notificationService.deleteNotification(id, userId);
        }
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Bütün bildirişləri silin", description = "İstifadəçinin bütün bildirişlərini silir.")
    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllNotifications(@AuthenticationPrincipal Object principal) {
        Long userId = extractUserId(principal);
        if (userId != null) {
            notificationService.deleteAllNotifications(userId);
        }
        return ResponseEntity.ok().build();
    }

    private Long extractUserId(Object principal) {
        if (principal instanceof Long) {
            return (Long) principal;
        }
        return null;
    }
}
