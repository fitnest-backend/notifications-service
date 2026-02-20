package az.fitnest.notifications.controller;

import az.fitnest.notifications.dto.DeviceRegistrationRequest;
import az.fitnest.notifications.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
@Slf4j
public class DeviceController {

    private final NotificationService notificationService;

    @PostMapping("/register")
    public ResponseEntity<Void> registerDevice(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody DeviceRegistrationRequest request) {
        
        log.info("Received device registration request for user {}", userId);
        notificationService.registerDevice(userId, request);
        return ResponseEntity.ok().build();
    }
}
