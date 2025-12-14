package gamerent.service;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import app.getxray.xray.junit.customjunitxml.annotations.XrayTest;
import gamerent.data.BookingRepository;
import gamerent.data.BookingRequest;
import gamerent.data.BookingStatus;
import gamerent.data.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Requirement("US2, TGR-21")
class PaymentServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private StripeGateway stripeGateway;

    @InjectMocks
    private PaymentService paymentService;

    private BookingRequest booking;

    @BeforeEach
    void setUp() {
        booking = new BookingRequest();
        booking.setId(10L);
        booking.setItemId(5L);
        booking.setUserId(99L);
        booking.setStartDate(LocalDate.now().plusDays(5));
        booking.setEndDate(LocalDate.now().plusDays(6));
        booking.setTotalPrice(12.34);
        booking.setStatus(BookingStatus.APPROVED);
        booking.setPaymentStatus(PaymentStatus.UNPAID);
    }

    @Test
    @XrayTest(key = "PAY-UNIT-1")
    @Tag("unit")
    void createCheckoutSession_ShouldThrow_WhenBookingNotApproved() {
        booking.setStatus(BookingStatus.PENDING);
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        assertThrows(RuntimeException.class, () ->
                paymentService.createCheckoutSession(10L, 99L, "http://localhost:5173"));

        verifyNoInteractions(stripeGateway);
    }

    @Test
    @XrayTest(key = "PAY-UNIT-2")
    @Tag("unit")
    void createCheckoutSession_ShouldThrow_WhenNotRenter() {
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        assertThrows(RuntimeException.class, () ->
                paymentService.createCheckoutSession(10L, 123L, "http://localhost:5173"));

        verifyNoInteractions(stripeGateway);
    }

    @Test
    @XrayTest(key = "PAY-UNIT-3")
    @Tag("unit")
    void createCheckoutSession_ShouldPersistStripeSessionIds() {
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(BookingRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stripeGateway.createCheckoutSession(any())).thenReturn(
                new StripeCheckoutSession("cs_test_123", "https://example.test/checkout", "unpaid", "pi_test_123", 1234L)
        );

        StripeCheckoutSession session = paymentService.createCheckoutSession(10L, 99L, "http://localhost:5173/");

        assertEquals("cs_test_123", session.id());
        assertEquals("https://example.test/checkout", session.url());
        assertEquals("cs_test_123", booking.getStripeCheckoutSessionId());
        assertEquals("pi_test_123", booking.getStripePaymentIntentId());
        verify(bookingRepository, times(1)).save(any(BookingRequest.class));
    }

    @Test
    @XrayTest(key = "PAY-UNIT-4")
    @Tag("unit")
    void confirmPayment_ShouldMarkBookingAsPaid_WhenStripeSaysPaid() {
        booking.setStripeCheckoutSessionId("cs_test_123");
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(BookingRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stripeGateway.retrieveCheckoutSession("cs_test_123")).thenReturn(
                new StripeCheckoutSession("cs_test_123", null, "paid", "pi_test_999", 1234L)
        );

        BookingRequest updated = paymentService.confirmPayment(10L, "cs_test_123", 99L);

        assertEquals(PaymentStatus.PAID, updated.getPaymentStatus());
        assertNotNull(updated.getPaidAt());
        assertEquals("pi_test_999", updated.getStripePaymentIntentId());
    }

    @Test
    @XrayTest(key = "PAY-UNIT-5")
    @Tag("unit")
    void confirmPayment_ShouldThrow_WhenStripeSaysUnpaid() {
        booking.setStripeCheckoutSessionId("cs_test_123");
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(stripeGateway.retrieveCheckoutSession("cs_test_123")).thenReturn(
                new StripeCheckoutSession("cs_test_123", null, "unpaid", "pi_test_999", 1234L)
        );

        assertThrows(RuntimeException.class, () -> paymentService.confirmPayment(10L, "cs_test_123", 99L));
        assertEquals(PaymentStatus.UNPAID, booking.getPaymentStatus());
    }

    @Test
    @XrayTest(key = "PAY-UNIT-6")
    @Tag("unit")
    void createCheckoutSession_ShouldCancelBooking_WhenPaymentWindowExpired() {
        booking.setPaymentDueAt(java.time.LocalDateTime.now().minusMinutes(1));
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(BookingRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                paymentService.createCheckoutSession(10L, 99L, "http://localhost:5173"));

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertTrue(ex.getMessage().toLowerCase().contains("expired"));
        verify(bookingRepository, atLeastOnce()).save(any(BookingRequest.class));
        verifyNoInteractions(stripeGateway);
    }
}


