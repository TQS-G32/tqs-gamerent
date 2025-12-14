package gamerent.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class StripeGatewayImpl implements StripeGateway {

    private final String secretKey;

    public StripeGatewayImpl(@Value("${stripe.secretKey:}") String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    public StripeCheckoutSession createCheckoutSession(StripeCheckoutSessionCreateRequest request) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("Stripe secret key not configured");
        }

        Stripe.apiKey = secretKey;

        SessionCreateParams.LineItem.PriceData.ProductData productData =
                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                        .setName(request.productName())
                        .build();

        SessionCreateParams.LineItem.PriceData priceData =
                SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency(request.currency())
                        .setUnitAmount(request.amountCents())
                        .setProductData(productData)
                        .build();

        SessionCreateParams.LineItem lineItem =
                SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(priceData)
                        .build();

        SessionCreateParams.Builder params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(request.successUrl())
                .setCancelUrl(request.cancelUrl())
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .addLineItem(lineItem);

        Map<String, String> metadata = request.metadata();
        if (metadata != null) {
            metadata.forEach(params::putMetadata);
        }

        try {
            Session session = Session.create(params.build());
            return new StripeCheckoutSession(
                    session.getId(),
                    session.getUrl(),
                    session.getPaymentStatus(),
                    session.getPaymentIntent(),
                    session.getAmountTotal()
            );
        } catch (StripeException e) {
            throw new RuntimeException("Stripe error creating checkout session: " + e.getMessage(), e);
        }
    }

    @Override
    public StripeCheckoutSession retrieveCheckoutSession(String sessionId) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("Stripe secret key not configured");
        }
        Stripe.apiKey = secretKey;
        try {
            Session session = Session.retrieve(sessionId);
            return new StripeCheckoutSession(
                    session.getId(),
                    session.getUrl(),
                    session.getPaymentStatus(),
                    session.getPaymentIntent(),
                    session.getAmountTotal()
            );
        } catch (StripeException e) {
            throw new RuntimeException("Stripe error retrieving checkout session: " + e.getMessage(), e);
        }
    }
}


