package az.fitnest.notifications.controller;

import az.fitnest.notifications.dto.NotificationTemplateResponse;
import az.fitnest.notifications.model.enums.NotificationChannel;
import az.fitnest.notifications.service.NotificationTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/notification-templates")
@RequiredArgsConstructor
@Tag(name = "Admin Notification Templates", description = "PUSH/SMS bildiriş şablonlarını idarə etmək üçün ucluqlar")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class NotificationTemplateAdminController {

    private final NotificationTemplateService notificationTemplateService;

    @Operation(summary = "Bildiriş şablonlarını əldə edin", description = "Kanal üzrə (PUSH/SMS) bildiriş şablonlarını qaytarır.")
    @GetMapping
    public ResponseEntity<List<NotificationTemplateResponse>> getTemplates(@RequestParam(required = false) NotificationChannel channel) {
        return ResponseEntity.ok(notificationTemplateService.getTemplates(channel));
    }
}

