package gamerent.service;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import app.getxray.xray.junit.customjunitxml.annotations.XrayTest;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Requirement("US2, TGR-21")
class StripeGatewayImplTest {

    @Test
    @XrayTest(key = "PAY-UNIT-7")
    @Tag("unit")
    void createCheckoutSession_ShouldThrow_WhenSecretKeyMissing() {
        StripeGatewayImpl gateway = new StripeGatewayImpl("  ");
        StripeCheckoutSessionCreateRequest req = new StripeCheckoutSessionCreateRequest(
                1234L,
                "eur",
                "Test product",
                "http://localhost/success",
                "http://localhost/cancel",
                Map.of("bookingId", "10")
        );

        assertThrows(IllegalStateException.class, () -> gateway.createCheckoutSession(req));
    }

    @Test
    @XrayTest(key = "PAY-UNIT-8")
    @Tag("unit")
    void createCheckoutSession_ShouldMapStripeSessionFields() {
        StripeGatewayImpl gateway = new StripeGatewayImpl("sk_test_123");

        StripeCheckoutSessionCreateRequest req = new StripeCheckoutSessionCreateRequest(
                1234L,
                "eur",
                "Test product",
                "http://localhost/success",
                "http://localhost/cancel",
                Map.of("bookingId", "10", "env", "test")
        );

        Session session = mock(Session.class);
        when(session.getId()).thenReturn("cs_test_123");
        when(session.getUrl()).thenReturn("https://stripe.test/checkout/cs_test_123");
        when(session.getPaymentStatus()).thenReturn("unpaid");
        when(session.getPaymentIntent()).thenReturn("pi_test_123");
        when(session.getAmountTotal()).thenReturn(1234L);

        try (MockedStatic<Session> mocked = Mockito.mockStatic(Session.class)) {
            mocked.when(() -> Session.create(any(SessionCreateParams.class))).thenReturn(session);

            StripeCheckoutSession out = gateway.createCheckoutSession(req);
            assertEquals("cs_test_123", out.id());
            assertEquals("https://stripe.test/checkout/cs_test_123", out.url());
            assertEquals("unpaid", out.paymentStatus());
            assertEquals("pi_test_123", out.paymentIntentId());
            assertEquals(1234L, out.amountTotal());
        }
    }

    @Test
    @XrayTest(key = "PAY-UNIT-9")
    @Tag("unit")
    void createCheckoutSession_ShouldWrapStripeException() {
        StripeGatewayImpl gateway = new StripeGatewayImpl("sk_test_123");
        StripeCheckoutSessionCreateRequest req = new StripeCheckoutSessionCreateRequest(
                500L,
                "eur",
                "Test product",
                "http://localhost/success",
                "http://localhost/cancel",
                null
        );

        StripeException stripeEx = new StripeException("boom", null, null, 0, null) {};

        try (MockedStatic<Session> mocked = Mockito.mockStatic(Session.class)) {
            mocked.when(() -> Session.create(any(SessionCreateParams.class))).thenThrow(stripeEx);

            RuntimeException ex = assertThrows(RuntimeException.class, () -> gateway.createCheckoutSession(req));
            assertTrue(ex.getMessage().toLowerCase().contains("stripe error creating checkout session"));
            assertSame(stripeEx, ex.getCause());
        }
    }

    @Test
    @XrayTest(key = "PAY-UNIT-10")
    @Tag("unit")
    void retrieveCheckoutSession_ShouldMapStripeSessionFields() {
        StripeGatewayImpl gateway = new StripeGatewayImpl("sk_test_123");

        Session session = mock(Session.class);
        when(session.getId()).thenReturn("cs_test_999");
        when(session.getUrl()).thenReturn(null);
        when(session.getPaymentStatus()).thenReturn("paid");
        when(session.getPaymentIntent()).thenReturn("pi_test_999");
        when(session.getAmountTotal()).thenReturn(3000L);

        try (MockedStatic<Session> mocked = Mockito.mockStatic(Session.class)) {
            mocked.when(() -> Session.retrieve("cs_test_999")).thenReturn(session);

            StripeCheckoutSession out = gateway.retrieveCheckoutSession("cs_test_999");
            assertEquals("cs_test_999", out.id());
            assertNull(out.url());
            assertEquals("paid", out.paymentStatus());
            assertEquals("pi_test_999", out.paymentIntentId());
            assertEquals(3000L, out.amountTotal());
        }
    }

    @Test
    @XrayTest(key = "PAY-UNIT-11")
    @Tag("unit")
    void retrieveCheckoutSession_ShouldWrapStripeException() {
        StripeGatewayImpl gateway = new StripeGatewayImpl("sk_test_123");

        StripeException stripeEx = new StripeException("boom", null, null, 0, null) {};

        try (MockedStatic<Session> mocked = Mockito.mockStatic(Session.class)) {
            mocked.when(() -> Session.retrieve("cs_test_err")).thenThrow(stripeEx);

            RuntimeException ex = assertThrows(RuntimeException.class, () -> gateway.retrieveCheckoutSession("cs_test_err"));
            assertTrue(ex.getMessage().toLowerCase().contains("stripe error retrieving checkout session"));
            assertSame(stripeEx, ex.getCause());
        }
    }
}


