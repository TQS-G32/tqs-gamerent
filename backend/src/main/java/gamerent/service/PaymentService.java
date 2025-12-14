package gamerent.service;

import gamerent.data.BookingRepository;
import gamerent.data.BookingRequest;
import gamerent.data.BookingStatus;
import gamerent.data.PaymentStatus;
import gamerent.config.BookingValidationException;
import gamerent.config.PaymentException;
import gamerent.config.UnauthorizedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class PaymentService {

    private final BookingRepository bookingRepository;
    private final StripeGateway stripeGateway;

    public PaymentService(BookingRepository bookingRepository, StripeGateway stripeGateway) {
        this.bookingRepository = bookingRepository;
        this.stripeGateway = stripeGateway;
    }

    public StripeCheckoutSession createCheckoutSession(Long bookingId, Long currentUserId, String frontendBaseUrl) {
        BookingRequest booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        validateBookingOwnership(booking, currentUserId);
        validateBookingApprovedAndUnpaid(booking);
        expireIfPastDue(booking);

        long amountCents = toCents(booking.getTotalPrice());
        String baseUrl = normalizeBaseUrl(frontendBaseUrl);

        String successUrl = baseUrl + "/bookings?payment_success=1&bookingId=" + bookingId
                + "&session_id={CHECKOUT_SESSION_ID}";
        String cancelUrl = baseUrl + "/bookings?payment_cancelled=1&bookingId=" + bookingId;

        StripeCheckoutSessionCreateRequest request = new StripeCheckoutSessionCreateRequest(
                amountCents,
                "eur",
                "GameRent booking #" + bookingId,
                successUrl,
                cancelUrl,
                Map.of(
                        "bookingId", String.valueOf(bookingId),
                        "userId", String.valueOf(currentUserId)
                )
        );

        StripeCheckoutSession session = stripeGateway.createCheckoutSession(request);

        booking.setStripeCheckoutSessionId(session.id());
        booking.setStripePaymentIntentId(session.paymentIntentId());
        bookingRepository.save(booking);

        return session;
    }

    public BookingRequest confirmPayment(Long bookingId, String sessionId, Long currentUserId) {
        BookingRequest booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        validateBookingOwnership(booking, currentUserId);
        if (booking.getStatus() != BookingStatus.APPROVED) {
            throw new BookingValidationException("Booking is not approved");
        }
        expireIfPastDue(booking);
        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            return booking;
        }
        if (booking.getStripeCheckoutSessionId() == null || !booking.getStripeCheckoutSessionId().equals(sessionId)) {
            throw new PaymentException("Invalid Stripe session for booking");
        }

        StripeCheckoutSession session = stripeGateway.retrieveCheckoutSession(sessionId);
        if (session.paymentStatus() == null || !"paid".equalsIgnoreCase(session.paymentStatus())) {
            throw new PaymentException("Payment not completed");
        }

        booking.setPaymentStatus(PaymentStatus.PAID);
        booking.setPaidAt(LocalDateTime.now());
        if (session.paymentIntentId() != null) {
            booking.setStripePaymentIntentId(session.paymentIntentId());
        }
        return bookingRepository.save(booking);
    }

    private void validateBookingOwnership(BookingRequest booking, Long currentUserId) {
        if (currentUserId == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        if (booking.getUserId() == null || !booking.getUserId().equals(currentUserId)) {
            throw new UnauthorizedException("Unauthorized: You are not the renter of this booking");
        }
    }

    private void validateBookingApprovedAndUnpaid(BookingRequest booking) {
        if (booking.getStatus() != BookingStatus.APPROVED) {
            throw new BookingValidationException("Booking is not approved");
        }
        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BookingValidationException("Booking already paid");
        }
    }

    private void expireIfPastDue(BookingRequest booking) {
        if (booking.getStatus() != BookingStatus.APPROVED) return;
        if (booking.getPaymentStatus() == PaymentStatus.PAID) return;
        LocalDateTime dueAt = booking.getPaymentDueAt();
        if (dueAt != null && LocalDateTime.now().isAfter(dueAt)) {
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            throw new PaymentException("Payment window expired. Booking cancelled.");
        }
    }

    private long toCents(Double totalPrice) {
        double value = totalPrice == null ? 0.0 : totalPrice;
        if (value < 0) value = 0.0;
        return Math.round(value * 100.0);
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) return "";
        if (baseUrl.endsWith("/")) return baseUrl.substring(0, baseUrl.length() - 1);
        return baseUrl;
    }
}
