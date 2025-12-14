package gamerent.boundary;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import app.getxray.xray.junit.customjunitxml.annotations.XrayTest;
import gamerent.service.PaymentService;
import gamerent.service.StripeCheckoutSession;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Requirement("US2, TGR-21")
class PaymentControllerTest {

    @Test
    @XrayTest(key = "PAY-UNIT-12")
    @Tag("unit")
    void createCheckoutSession_ShouldUseOriginHeader_WhenPresent() throws Exception {
        PaymentService paymentService = mock(PaymentService.class);
        when(paymentService.createCheckoutSession(eq(10L), eq(99L), eq("http://origin.test")))
                .thenReturn(new StripeCheckoutSession("cs_test_123", "https://stripe.test/checkout", "unpaid", "pi_test_123", 1234L));

        PaymentController controller = new PaymentController(paymentService, "http://fallback.test");
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(post("/api/payments/checkout-session")
                        .header("Origin", "http://origin.test")
                        .sessionAttr("userId", 99) // Integer should be accepted too
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookingId\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://stripe.test/checkout"))
                .andExpect(jsonPath("$.sessionId").value("cs_test_123"));
    }

    @Test
    @XrayTest(key = "PAY-UNIT-13")
    @Tag("unit")
    void createCheckoutSession_ShouldFallbackToConfiguredBaseUrl_WhenOriginMissing() throws Exception {
        PaymentService paymentService = mock(PaymentService.class);
        when(paymentService.createCheckoutSession(eq(10L), eq(99L), eq("http://fallback.test")))
                .thenReturn(new StripeCheckoutSession("cs_test_123", "https://stripe.test/checkout", "unpaid", "pi_test_123", 1234L));

        PaymentController controller = new PaymentController(paymentService, "http://fallback.test");
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(post("/api/payments/checkout-session")
                        .sessionAttr("userId", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookingId\":10}"))
                .andExpect(status().isOk());

        verify(paymentService, times(1)).createCheckoutSession(eq(10L), eq(99L), eq("http://fallback.test"));
    }

    @Test
    @XrayTest(key = "PAY-UNIT-14")
    @Tag("unit")
    void createCheckoutSession_ShouldReturnBadRequest_OnRuntimeException() throws Exception {
        PaymentService paymentService = mock(PaymentService.class);
        when(paymentService.createCheckoutSession(anyLong(), anyLong(), anyString()))
                .thenThrow(new RuntimeException("boom"));

        PaymentController controller = new PaymentController(paymentService, "http://fallback.test");
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(post("/api/payments/checkout-session")
                        .sessionAttr("userId", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookingId\":10}"))
                .andExpect(status().isBadRequest());
    }
}


