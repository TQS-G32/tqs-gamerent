package gamerent.service;

public record StripeCheckoutSession(
        String id,
        String url,
        String paymentStatus,
        String paymentIntentId,
        Long amountTotal
) {}


