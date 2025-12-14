package gamerent.boundary;

import gamerent.data.BookingRequest;
import gamerent.service.PaymentService;
import gamerent.service.StripeCheckoutSession;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {
    private static final Logger logger = Logger.getLogger(PaymentController.class.getName());
    private static final String USER_ID_KEY = "userId";

    private final PaymentService paymentService;
    private final String frontendBaseUrl;

    public PaymentController(PaymentService paymentService,
                             @Value("${app.frontendBaseUrl:http://localhost:5173}") String frontendBaseUrl) {
        this.paymentService = paymentService;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public record CheckoutSessionRequest(Long bookingId) {}
    public record CheckoutSessionResponse(String url, String sessionId) {}
    public record ConfirmPaymentRequest(Long bookingId, String sessionId) {}

    @PostMapping("/checkout-session")
    public CheckoutSessionResponse createCheckoutSession(@RequestBody CheckoutSessionRequest body,
                                                         HttpServletRequest request) {
        Long userId = resolveCurrentUserId(request);
        String baseUrl = resolveFrontendBaseUrl(request);
        logger.log(Level.INFO, "Checkout session creation - User: {0}, Booking: {1}", 
            new Object[]{userId, body.bookingId()});
        try {
            StripeCheckoutSession session = paymentService.createCheckoutSession(body.bookingId(), userId, baseUrl);
            logger.log(Level.INFO, "Checkout session created successfully - Booking: {0}, Session: {1}", 
                new Object[]{body.bookingId(), session.id()});
            return new CheckoutSessionResponse(session.url(), session.id());
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/confirm")
    public BookingRequest confirmPayment(@RequestBody ConfirmPaymentRequest body, HttpServletRequest request) {
        Long userId = resolveCurrentUserId(request);
        logger.log(Level.INFO, "Payment confirmation attempt - User: {0}, Booking: {1}, Session: {2}", 
            new Object[]{userId, body.bookingId(), body.sessionId()});
        try {
            BookingRequest confirmed = paymentService.confirmPayment(body.bookingId(), body.sessionId(), userId);
            logger.log(Level.INFO, "Payment confirmed successfully - Booking: {0}, Amount: {1}", 
                new Object[]{body.bookingId(), confirmed.getTotalPrice()});
            return confirmed;
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private Long resolveCurrentUserId(HttpServletRequest request) {
        Object uid = request.getSession(false) != null ? request.getSession(false).getAttribute(USER_ID_KEY) : null;
        if (uid instanceof Long longValue) return longValue;
        if (uid instanceof Integer intValue) return intValue.longValue();
        return null;
    }

    private String resolveFrontendBaseUrl(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isBlank()) return origin;
        // Some deployments won't send Origin (e.g., server-side calls), fallback to config
        return frontendBaseUrl;
    }
}


