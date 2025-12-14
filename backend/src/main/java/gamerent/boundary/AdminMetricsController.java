package gamerent.boundary;

import gamerent.boundary.dto.AdminMetricsResponse;
import gamerent.data.UserRepository;
import gamerent.data.ItemRepository;
import gamerent.data.BookingRepository;
import gamerent.data.BookingRequest;
import gamerent.data.BookingStatus;
import gamerent.data.PaymentStatus;
import gamerent.data.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminMetricsController {
    private static final Logger logger = Logger.getLogger(AdminMetricsController.class.getName());
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final BookingRepository bookingRepository;

    @Autowired
    public AdminMetricsController(UserRepository userRepository, ItemRepository itemRepository, BookingRepository bookingRepository) {
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.bookingRepository = bookingRepository;
    }

    @GetMapping("/metrics")
    //@PreAuthorize("hasRole('ADMIN')") // Uncomment if using Spring Security method security
    public ResponseEntity<AdminMetricsResponse> getMetrics(HttpServletRequest request) {
        // --- Access Control: Only allow ADMINs ---
        Object role = request.getSession(false) != null ? request.getSession(false).getAttribute("userRole") : null;
        if (role == null || !"ADMIN".equals(role.toString())) {
            logger.log(Level.WARNING, "Unauthorized admin dashboard access attempt - Role: {0}", role);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        // --- Auditing: Log access ---
        Object adminId = request.getSession(false) != null ? request.getSession(false).getAttribute("userId") : null;
        logger.log(Level.INFO, "[AUDIT] Admin dashboard accessed by adminId={0} at {1}", 
            new Object[]{adminId, LocalDateTime.now()});

        // --- Metrics ---
        int totalAccounts = (int) userRepository.count();
        int activeListings = (int) itemRepository.count();
        int totalBookings = (int) bookingRepository.count();
        double monthlyRevenue = calculateMonthlyRevenue();
        int openIssues = 0; // No disputes/issues entity found

        logger.log(Level.INFO, "Admin metrics calculated - Accounts: {0}, Listings: {1}, Bookings: {2}, Revenue: ${3}", 
            new Object[]{totalAccounts, activeListings, totalBookings, monthlyRevenue});

        AdminMetricsResponse resp = new AdminMetricsResponse(totalAccounts, activeListings, totalBookings, monthlyRevenue, openIssues);
        return ResponseEntity.ok(resp);
    }

    private double calculateMonthlyRevenue() {
        YearMonth now = YearMonth.now(ZoneId.systemDefault());
        List<BookingRequest> bookings = bookingRepository.findAll();
        double revenue = bookings.stream()
            .filter(b -> b.getStatus() == BookingStatus.APPROVED && b.getPaymentStatus() == PaymentStatus.PAID)
            .filter(b -> b.getPaidAt() != null && YearMonth.from(b.getPaidAt().toLocalDate()).equals(now))
            .mapToDouble(b -> b.getTotalPrice() != null ? b.getTotalPrice() * 0.2 : 0.0)
            .sum();
        return Math.round(revenue * 100.0) / 100.0;
    }
}
