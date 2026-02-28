package az.fitnest.notifications.controller;

import az.fitnest.notifications.dto.DeviceDto;
import az.fitnest.notifications.dto.DeviceRegistrationRequest;
import az.fitnest.notifications.dto.DirectPushRequest;
import az.fitnest.notifications.repository.DeviceRepository;
import az.fitnest.notifications.service.NotificationService;
import az.fitnest.notifications.util.DeviceDetector;
import az.fitnest.notifications.model.entity.Device;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
@Slf4j
public class DeviceController {

    private final NotificationService notificationService;
    private final DeviceRepository deviceRepository;

    @PostMapping("/register")
    public ResponseEntity<Void> registerDevice(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody DeviceRegistrationRequest request) {
        
        Device.Platform platform = DeviceDetector.detectPlatform();
        if (platform == null) {
            log.warn("Could not detect platform from User-Agent for user {}", userId);
            // Defaulting to ANDROID or returning error? 
            // Most clients use Android/iOS strings. Defaulting to ANDROID for now or just letting it be null if DB allows?
            // DB has nullable=false. Let's default to ANDROID if undetected but logged.
            platform = Device.Platform.ANDROID;
        }

        notificationService.registerDevice(userId, request.getPushToken(), platform);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DeviceDto>> getAllDevices() {
        log.info("Admin request to get all registered devices");
        List<DeviceDto> devices = deviceRepository.findAll().stream()
                .map(device -> DeviceDto.builder()
                        .deviceId(device.getDeviceId())
                        .userId(device.getUserId())
                        .pushToken(device.getPushToken())
                        .platform(device.getPlatform())
                        .createdAt(device.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(devices);
    }

    @PostMapping("/send")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sendPushToUser(@Valid @RequestBody DirectPushRequest request) {
        log.info("Admin request to send notification to user {}", request.getUserId());
        notificationService.sendToUser(request.getUserId(), request.getTitle(), request.getBody());
        return ResponseEntity.ok().build();
    }
}
