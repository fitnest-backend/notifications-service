package az.fitnest.notifications.service.impl;

import az.fitnest.notifications.exception.AdminCustomerNotFoundException;
import az.fitnest.notifications.exception.NotificationSendFailedException;
import az.fitnest.notifications.grpc.IdentityUserGrpcClient;
import az.fitnest.notifications.service.CustomerQrScanQueryService;
import az.fitnest.notifications.service.OutgoingRequestAuthService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CustomerQrScanQueryServiceImpl implements CustomerQrScanQueryService {

    @Value("${order.backend.url:http://order-backend:8080}")
    private String orderBackendUrl;
    private final IdentityUserGrpcClient identityUserGrpcClient;
    private final OutgoingRequestAuthService outgoingRequestAuthService;

    @Override
    public JsonNode getCustomerQrScanHistory(Long customerId, LocalDateTime from, LocalDateTime to) {
        try {
            if (!identityUserGrpcClient.userExists(customerId)) {
                throw new AdminCustomerNotFoundException();
            }
            String url = UriComponentsBuilder
                    .fromPath("/api/v1/admin/customers/{customerId}/qr-scan-history")
                    .queryParamIfPresent("from", java.util.Optional.ofNullable(from))
                    .queryParamIfPresent("to", java.util.Optional.ofNullable(to))
                    .buildAndExpand(customerId)
                    .toUriString();
            String authHeader = outgoingRequestAuthService.getIncomingAuthorizationHeader();

            return WebClient.builder()
                    .baseUrl(orderBackendUrl)
                    .build()
                    .get()
                    .uri(url)
                    .headers(h -> {
                        if (authHeader != null && !authHeader.isBlank()) {
                            h.set(HttpHeaders.AUTHORIZATION, authHeader);
                        }
                    })
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response -> {
                        if (response.statusCode().value() == 404) {
                            return response.createException().flatMap(ex -> reactor.core.publisher.Mono.error(new AdminCustomerNotFoundException()));
                        }
                        return response.createException();
                    })
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (AdminCustomerNotFoundException ex) {
            throw ex;
        } catch (WebClientResponseException.NotFound ex) {
            throw new AdminCustomerNotFoundException();
        } catch (Exception ex) {
            throw new NotificationSendFailedException(ex.getMessage());
        }
    }
}

