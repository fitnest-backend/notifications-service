package az.fitnest.notifications.controller;

import az.fitnest.notifications.dto.DeviceDto;
import az.fitnest.notifications.dto.DirectPushRequest;
import az.fitnest.notifications.repository.DeviceRepository;
import az.fitnest.notifications.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/devices")
@RequiredArgsConstructor
@Tag(name = "Admin Devices", description = "Admin endpoints for device management")
@SecurityRequirement(name = "bearerAuth")
public class AdminDeviceController {
    private final NotificationService notificationService;
    private final DeviceRepository deviceRepository;

    @Operation(summary = "Bütün cihazları əldə edin (Admin)", description = "Sistemdə qeydiyyatdan keçmiş bütün cihazların siyahısını qaytarır. Admin rolu tələb olunur.")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DeviceDto>> getAllDevices() {
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
        notificationService.sendToDevice(request.deviceId(), request.title(), request.body());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Bütün cihazları sil (Admin)", description = "Sistemdəki bütün cihazları silir. Admin rolu tələb olunur.")
    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAllDevices() {
        deviceRepository.deleteAll();
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "İstifadəçinin bütün cihazlarını sil (Admin)", description = "Verilmiş istifadəçi ID-sinə aid bütün cihazları silir. Admin rolu tələb olunur.")
    @DeleteMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDevicesByUserId(@PathVariable Long userId) {
        deviceRepository.deleteByUserId(userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Cihazı sil (Admin)", description = "Verilmiş deviceId ilə cihazı silir. Admin rolu tələb olunur.")
    @DeleteMapping("/{deviceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDeviceById(@PathVariable Long deviceId) {
        deviceRepository.deleteById(deviceId);
        return ResponseEntity.noContent().build();
    }
}

