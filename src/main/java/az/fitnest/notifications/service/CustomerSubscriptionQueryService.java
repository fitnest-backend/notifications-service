package az.fitnest.notifications.service;

import com.fasterxml.jackson.databind.JsonNode;

public interface CustomerSubscriptionQueryService {
    JsonNode getCustomerSubscription(Long customerId);
}

