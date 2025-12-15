package gamerent.boundary;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import app.getxray.xray.junit.customjunitxml.annotations.XrayTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gamerent.data.*;
import gamerent.service.StripeCheckoutSession;
import gamerent.service.StripeCheckoutSessionCreateRequest;
import gamerent.service.StripeGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@Requirement("US2, TGR-21")
class PaymentControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;
    private User renter;
    private Item item;
    private BookingRequest booking;

    @BeforeEach
    void setUp() {
        cleanupUser("owner-pay@test.com");
        cleanupUser("renter-pay@test.com");

        owner = new User();
        owner.setName("Pay Owner");
        owner.setEmail("owner-pay@test.com");
        owner.setPassword("password");
        owner.setRole("OWNER");
        owner = userRepository.save(owner);

        renter = new User();
        renter.setName("Pay Renter");
        renter.setEmail("renter-pay@test.com");
        renter.setPassword("password");
        renter.setRole("RENTER");
        renter = userRepository.save(renter);

        item = new Item();
        item.setName("Payment Item");
        item.setDescription("A payment test item");
        item.setCategory("Console");
        item.setPricePerDay(10.0);
        item.setAvailable(true);
        item.setOwner(owner);
        item = itemRepository.save(item);

        booking = new BookingRequest();
        booking.setItemId(item.getId());
        booking.setUserId(renter.getId());
        booking.setStartDate(LocalDate.of(2035, 12, 1));
        booking.setEndDate(LocalDate.of(2035, 12, 3));
        booking.setStatus(BookingStatus.APPROVED);
        booking.setPaymentStatus(PaymentStatus.UNPAID);
        booking.setTotalPrice(30.0);
        booking = bookingRepository.save(booking);
    }

    private void cleanupUser(String email) {
        userRepository.findByEmail(email).ifPresent(u -> {
            // Delete items owned by user and their bookings
            List<Item> items = itemRepository.findByOwnerId(u.getId());
            for (Item i : items) {
                List<BookingRequest> bookings = bookingRepository.findByItemId(i.getId());
                bookingRepository.deleteAll(bookings);
                itemRepository.delete(i);
            }
            // Delete bookings made by user
            List<BookingRequest> userBookings = bookingRepository.findByUserId(u.getId());
            bookingRepository.deleteAll(userBookings);
            userRepository.delete(u);
        });
    }

    @Test
    @XrayTest(key = "TGR-36")
    @Tag("integration")
    void createCheckoutSession_ThenConfirm_ShouldMarkBookingPaid() throws Exception {
        // 1) Create checkout session
        String createBody = objectMapper.writeValueAsString(
                java.util.Map.of("bookingId", booking.getId())
        );

        String createResponse = mockMvc.perform(post("/api/payments/checkout-session")
                        .sessionAttr("userId", renter.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").isString())
                .andExpect(jsonPath("$.sessionId").value("cs_test_123"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode node = objectMapper.readTree(createResponse);
        String sessionId = node.get("sessionId").asText();

        BookingRequest persistedAfterCreate = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(persistedAfterCreate.getStripeCheckoutSessionId()).isEqualTo(sessionId);

        // 2) Confirm payment
        String confirmBody = objectMapper.writeValueAsString(
                java.util.Map.of("bookingId", booking.getId(), "sessionId", sessionId)
        );

        mockMvc.perform(post("/api/payments/confirm")
                        .sessionAttr("userId", renter.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("PAID"));

        BookingRequest persistedAfterConfirm = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(persistedAfterConfirm.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(persistedAfterConfirm.getPaidAt()).isNotNull();
    }

    @Test
    @XrayTest(key = "TGR-36")
    @Tag("integration")
    void createCheckoutSession_WhenExpired_ShouldCancelBookingAndReturnBadRequest() throws Exception {
        booking.setPaymentDueAt(java.time.LocalDateTime.now().minusMinutes(1));
        bookingRepository.save(booking);

        String createBody = objectMapper.writeValueAsString(
                java.util.Map.of("bookingId", booking.getId())
        );

        mockMvc.perform(post("/api/payments/checkout-session")
                        .sessionAttr("userId", renter.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isBadRequest());

        BookingRequest persisted = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @TestConfiguration
    static class StripeGatewayStubConfig {
        private final AtomicReference<String> lastCreatedSessionId = new AtomicReference<>("cs_test_123");

        @Bean
        @Primary
        StripeGateway stripeGateway() {
            return new StripeGateway() {
                @Override
                public StripeCheckoutSession createCheckoutSession(StripeCheckoutSessionCreateRequest request) {
                    // Always return deterministic values; tests should not hit Stripe.
                    return new StripeCheckoutSession(
                            lastCreatedSessionId.get(),
                            "https://stripe.test/checkout/" + lastCreatedSessionId.get(),
                            "unpaid",
                            "pi_test_123",
                            request.amountCents()
                    );
                }

                @Override
                public StripeCheckoutSession retrieveCheckoutSession(String sessionId) {
                    return new StripeCheckoutSession(
                            sessionId,
                            null,
                            "paid",
                            "pi_test_123",
                            3000L
                    );
                }
            };
        }
    }
}


