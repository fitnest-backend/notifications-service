package az.fitnest.notifications.controller;

import az.fitnest.notifications.dto.NotificationDto;
import az.fitnest.notifications.dto.PaginatedResponse;
import az.fitnest.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<PaginatedResponse<NotificationDto>> getUserNotifications(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PaginatedResponse.of(notificationService.getUserNotifications(userId, pageable)));
    }
}
