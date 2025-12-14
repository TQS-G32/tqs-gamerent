package gamerent.service;

import java.util.Map;

public record StripeCheckoutSessionCreateRequest(
        Long amountCents,
        String currency,
        String productName,
        String successUrl,
        String cancelUrl,
        Map<String, String> metadata
) {}


