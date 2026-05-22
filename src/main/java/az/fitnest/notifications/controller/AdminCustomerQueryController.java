package az.fitnest.notifications.controller;

import az.fitnest.notifications.exception.InvalidDateRangeException;
import az.fitnest.notifications.service.CustomerQrScanQueryService;
import az.fitnest.notifications.service.CustomerSubscriptionQueryService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/admin/customers")
@RequiredArgsConstructor
@Tag(name = "Admin Customer Queries", description = "Müştərinin subscription və QR history sorğuları")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCustomerQueryController {

    private final CustomerSubscriptionQueryService customerSubscriptionQueryService;
    private final CustomerQrScanQueryService customerQrScanQueryService;

    @Operation(summary = "Subscription", description = "Müştərinin aktiv abunəliyini qaytarır.")
    @GetMapping("/{customerId}/subscription")
    public ResponseEntity<JsonNode> getSubscription(@PathVariable Long customerId) {
        return ResponseEntity.ok(customerSubscriptionQueryService.getCustomerSubscription(customerId));
    }

    @Operation(summary = "QR History", description = "Müştərinin QR scan tarixçəsini qaytarır.")
    @GetMapping("/{customerId}/qr-scan-history")
    public ResponseEntity<JsonNode> getQrHistory(
            @PathVariable Long customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        if ((from == null) != (to == null)) {
            throw new InvalidDateRangeException("Date filters must be valid");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidDateRangeException("Date filters must be valid");
        }
        return ResponseEntity.ok(customerQrScanQueryService.getCustomerQrScanHistory(customerId, from, to));
    }
}

