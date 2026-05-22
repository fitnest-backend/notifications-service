package az.fitnest.notifications.service;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public interface CustomerQrScanQueryService {
    JsonNode getCustomerQrScanHistory(Long customerId, LocalDateTime from, LocalDateTime to);
}

