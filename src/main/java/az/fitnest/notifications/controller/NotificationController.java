package az.fitnest.notifications.controller;

import az.fitnest.notifications.dto.BroadcastPushRequest;
import az.fitnest.notifications.dto.NotificationDto;
import az.fitnest.notifications.dto.PaginatedResponse;
import az.fitnest.notifications.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<PaginatedResponse<NotificationDto>> getUserNotifications(
            @AuthenticationPrincipal Object principal,
            @PageableDefault(size = 20) Pageable pageable) {
        Long userId = extractUserId(principal);
        return ResponseEntity.ok(PaginatedResponse.of(notificationService.getUserNotifications(userId, pageable)));
    }

    @PostMapping("/broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> broadcast(@Valid @RequestBody BroadcastPushRequest request) {
        notificationService.broadcastPushNotification(request.getTitle(), request.getBody());
        return ResponseEntity.ok().build();
    }

    private Long extractUserId(Object principal) {
        if (principal instanceof Long) {
            return (Long) principal;
        }
        return null;
    }
}
