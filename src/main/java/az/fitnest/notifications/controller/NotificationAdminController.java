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

@RestController
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
@Tag(name = "Admin Notifications", description = "İstifadəçilərə yayım mesajlarını idarə etmək üçün administrativ ucluqlar")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class NotificationAdminController {

    private final NotificationService notificationService;

    @Operation(summary = "Yayım bildirişi göndərin (Admin)", description = "Bütün istifadəçilərə push bildirişi göndərir. Admin rolu tələb olunur.")
    @PostMapping("/broadcast")
    public ResponseEntity<Void> broadcast(@Valid @RequestBody BroadcastPushRequest request) {
        notificationService.broadcastPushNotification(request.title(), request.body());
        return ResponseEntity.ok().build();
    }
}
