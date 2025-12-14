package gamerent.service;

public interface StripeGateway {

    StripeCheckoutSession createCheckoutSession(StripeCheckoutSessionCreateRequest request);

    StripeCheckoutSession retrieveCheckoutSession(String sessionId);
}


