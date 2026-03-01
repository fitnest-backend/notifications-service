package az.fitnest.notifications.controller;

import az.fitnest.notifications.dto.DeviceDto;
import az.fitnest.notifications.dto.DeviceRegistrationRequest;
import az.fitnest.notifications.dto.DirectPushRequest;
import az.fitnest.notifications.repository.DeviceRepository;
import az.fitnest.notifications.service.NotificationService;
import az.fitnest.notifications.util.DeviceDetector;
import az.fitnest.notifications.model.entity.Device;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Devices", description = "İstifadəçi cihazlarını və push tokenlərini idarə etmək üçün ucluqlar")
@SecurityRequirement(name = "bearerAuth")
public class DeviceController {

    private final NotificationService notificationService;
    private final DeviceRepository deviceRepository;

    @Operation(summary = "Cihazı qeydiyyatdan keçirin", description = "İstifadəçinin cihazını push bildirişləri üçün qeydiyyatdan keçirir.")
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

    @Operation(summary = "Bütün cihazları əldə edin (Admin)", description = "Sistemdə qeydiyyatdan keçmiş bütün cihazların siyahısını qaytarır. Admin rolu tələb olunur.")
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

    @Operation(summary = "İstifadəçinin cihazlarını əldə edin", description = "Verilmiş istifadəçi ID-sinə aid olan bütün cihazları qaytarır. Admin rolu tələb olunur.")
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DeviceDto>> getDevicesByUserId(@PathVariable Long userId) {
        log.info("Admin request to get devices for user {}", userId);
        List<DeviceDto> devices = deviceRepository.findAllByUserId(userId).stream()
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

    @Operation(summary = "Cihaza push bildirişi göndərin (Admin)", description = "Xüsusi cihaza push bildirişi göndərir. Admin rolu tələb olunur.")
    @PostMapping("/send")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sendPushToDevice(@Valid @RequestBody DirectPushRequest request) {
        log.info("Admin request to send notification to device {}", request.getDeviceId());
        notificationService.sendToDevice(request.getDeviceId(), request.getTitle(), request.getBody());
        return ResponseEntity.ok().build();
    }
}
