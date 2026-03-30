package az.fitnest.notifications.controller;

import az.fitnest.notifications.dto.DeviceDto;
import az.fitnest.notifications.dto.DeviceRegistrationRequest;
import az.fitnest.notifications.dto.DirectPushRequest;
import az.fitnest.notifications.repository.DeviceRepository;
import az.fitnest.notifications.service.NotificationService;
import az.fitnest.notifications.util.DeviceDetector;
import az.fitnest.notifications.model.entity.Device;
import az.fitnest.notifications.model.enums.Platform;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
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

        Platform platform = DeviceDetector.detectPlatform();
        if (platform == null) {
            platform = Platform.ANDROID;
        }

        notificationService.registerDevice(userId, request.pushToken(), platform);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "İstifadəçinin cihazlarını əldə edin", description = "Verilmiş istifadəçi ID-sinə aid olan bütün cihazları qaytarır.")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<DeviceDto>> getDevicesByUserId(@PathVariable Long userId) {
        List<DeviceDto> devices = deviceRepository.findAllByUserId(userId).stream()
                .map(device -> DeviceDto.builder()
                        .deviceId(device.getDeviceId())
                        .userId(device.getUserId())
                        .pushToken(device.getPushToken())
                        .platform(device.getPlatform())
                        .createdAt(device.getCreatedAt())
                        .notificationsEnabled(device.getNotificationEnabled())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(devices);
    }

    @Operation(summary = "Cihaza push bildirişi göndərin", description = "Xüsusi cihaza push bildirişi göndərir.")
    @PostMapping("/send")
    public ResponseEntity<Void> sendPushToDevice(@Valid @RequestBody DirectPushRequest request) {
        notificationService.sendToDevice(request.deviceId(), request.title(), request.body());
        return ResponseEntity.ok().build();
    }
}
