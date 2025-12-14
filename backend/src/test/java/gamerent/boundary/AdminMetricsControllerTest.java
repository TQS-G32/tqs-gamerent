package gamerent.boundary;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import app.getxray.xray.junit.customjunitxml.annotations.XrayTest;
import gamerent.boundary.dto.AdminMetricsResponse;
import gamerent.data.*;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminMetricsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Requirement("ADMIN")
class AdminMetricsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private ItemRepository itemRepository;

    @MockBean
    private BookingRepository bookingRepository;

    @MockBean
    private DisputeRepository disputeRepository;

    private MockHttpSession adminSession;
    private MockHttpSession userSession;
    private MockHttpSession noSession;

    @BeforeEach
    void setUp() {
        // Admin session
        adminSession = new MockHttpSession();
        adminSession.setAttribute("userId", 1L);
        adminSession.setAttribute("userRole", "ADMIN");

        // Regular user session
        userSession = new MockHttpSession();
        userSession.setAttribute("userId", 2L);
        userSession.setAttribute("userRole", "USER");

        // No session
        noSession = new MockHttpSession();
    }

    @Test
    @XrayTest(key = "ADMIN-UNIT-1")
    @Tag("unit")
    void getMetrics_AsAdmin_ShouldReturnMetrics() throws Exception {
        // Given
        given(userRepository.count()).willReturn(10L);
        given(itemRepository.count()).willReturn(25L);
        given(bookingRepository.count()).willReturn(50L);
        given(bookingRepository.findAll()).willReturn(Collections.emptyList());
        given(disputeRepository.findByStatus(DisputeStatus.SUBMITTED)).willReturn(Collections.emptyList());
        given(disputeRepository.findByStatus(DisputeStatus.UNDER_REVIEW)).willReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/admin/metrics")
                .session(adminSession)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAccounts").value(10))
                .andExpect(jsonPath("$.activeListings").value(25))
                .andExpect(jsonPath("$.totalBookings").value(50))
                .andExpect(jsonPath("$.monthlyRevenue").value(0.0))
                .andExpect(jsonPath("$.openIssues").value(0));
    }

    @Test
    @XrayTest(key = "ADMIN-UNIT-2")
    @Tag("unit")
    void getMetrics_AsNonAdmin_ShouldReturnForbidden() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/admin/metrics")
                .session(userSession)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @XrayTest(key = "ADMIN-UNIT-3")
    @Tag("unit")
    void getMetrics_WithoutSession_ShouldReturnForbidden() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/admin/metrics")
                .session(noSession)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @XrayTest(key = "ADMIN-UNIT-4")
    @Tag("unit")
    void getMetrics_ShouldCalculateMonthlyRevenueCorrectly() throws Exception {
        // Given
        YearMonth currentMonth = YearMonth.now();
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        // Create bookings with different statuses
        BookingRequest paidBooking1 = createBooking(1L, BookingStatus.APPROVED, PaymentStatus.PAID, 100.0, now);
        BookingRequest paidBooking2 = createBooking(2L, BookingStatus.APPROVED, PaymentStatus.PAID, 200.0, now);
        BookingRequest unpaidBooking = createBooking(3L, BookingStatus.APPROVED, PaymentStatus.UNPAID, 150.0, null);
        BookingRequest pendingBooking = createBooking(4L, BookingStatus.PENDING, PaymentStatus.PAID, 100.0, now);
        BookingRequest lastMonthBooking = createBooking(5L, BookingStatus.APPROVED, PaymentStatus.PAID, 100.0, now.minusMonths(1));

        given(userRepository.count()).willReturn(5L);
        given(itemRepository.count()).willReturn(10L);
        given(bookingRepository.count()).willReturn(5L);
        given(bookingRepository.findAll()).willReturn(Arrays.asList(
                paidBooking1, paidBooking2, unpaidBooking, pendingBooking, lastMonthBooking
        ));
        given(disputeRepository.findByStatus(DisputeStatus.SUBMITTED)).willReturn(Collections.emptyList());
        given(disputeRepository.findByStatus(DisputeStatus.UNDER_REVIEW)).willReturn(Collections.emptyList());

        // Expected revenue: (100 + 200) * 0.2 = 60.0
        // When & Then
        mockMvc.perform(get("/api/admin/metrics")
                .session(adminSession)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAccounts").value(5))
                .andExpect(jsonPath("$.activeListings").value(10))
                .andExpect(jsonPath("$.totalBookings").value(5))
                .andExpect(jsonPath("$.monthlyRevenue").value(60.0));
    }

    @Test
    @XrayTest(key = "ADMIN-UNIT-5")
    @Tag("unit")
    void getMetrics_WithZeroData_ShouldReturnZeros() throws Exception {
        // Given
        given(userRepository.count()).willReturn(0L);
        given(itemRepository.count()).willReturn(0L);
        given(bookingRepository.count()).willReturn(0L);
        given(bookingRepository.findAll()).willReturn(Collections.emptyList());
        given(disputeRepository.findByStatus(DisputeStatus.SUBMITTED)).willReturn(Collections.emptyList());
        given(disputeRepository.findByStatus(DisputeStatus.UNDER_REVIEW)).willReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/admin/metrics")
                .session(adminSession)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAccounts").value(0))
                .andExpect(jsonPath("$.activeListings").value(0))
                .andExpect(jsonPath("$.totalBookings").value(0))
                .andExpect(jsonPath("$.monthlyRevenue").value(0.0))
                .andExpect(jsonPath("$.openIssues").value(0));
    }

    @Test
    @XrayTest(key = "ADMIN-UNIT-6")
    @Tag("unit")
    void getMetrics_WithNullTotalPrice_ShouldHandleGracefully() throws Exception {
        // Given
        LocalDateTime now = LocalDateTime.now();
        BookingRequest bookingWithNullPrice = createBooking(1L, BookingStatus.APPROVED, PaymentStatus.PAID, null, now);

        given(userRepository.count()).willReturn(1L);
        given(itemRepository.count()).willReturn(1L);
        given(bookingRepository.count()).willReturn(1L);
        given(bookingRepository.findAll()).willReturn(Collections.singletonList(bookingWithNullPrice));
        given(disputeRepository.findByStatus(DisputeStatus.SUBMITTED)).willReturn(Collections.emptyList());
        given(disputeRepository.findByStatus(DisputeStatus.UNDER_REVIEW)).willReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/admin/metrics")
                .session(adminSession)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyRevenue").value(0.0));
    }

    @Test
    @XrayTest(key = "ADMIN-UNIT-7")
    @Tag("unit")
    void getMetrics_WithMixedBookingStatuses_ShouldOnlyCountApprovedPaid() throws Exception {
        // Given
        LocalDateTime now = LocalDateTime.now();
        
        BookingRequest approved = createBooking(1L, BookingStatus.APPROVED, PaymentStatus.PAID, 100.0, now);
        BookingRequest rejected = createBooking(2L, BookingStatus.REJECTED, PaymentStatus.PAID, 100.0, now);
        BookingRequest cancelled = createBooking(3L, BookingStatus.CANCELLED, PaymentStatus.PAID, 100.0, now);

        given(userRepository.count()).willReturn(3L);
        given(itemRepository.count()).willReturn(3L);
        given(bookingRepository.count()).willReturn(3L);
        given(bookingRepository.findAll()).willReturn(Arrays.asList(approved, rejected, cancelled));
        given(disputeRepository.findByStatus(DisputeStatus.SUBMITTED)).willReturn(Collections.emptyList());
        given(disputeRepository.findByStatus(DisputeStatus.UNDER_REVIEW)).willReturn(Collections.emptyList());

        // Expected revenue: only approved booking = 100 * 0.2 = 20.0
        // When & Then
        mockMvc.perform(get("/api/admin/metrics")
                .session(adminSession)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyRevenue").value(20.0));
    }

    @Test
    @XrayTest(key = "ADMIN-UNIT-8")
    @Tag("unit")
    void getMetrics_WithOpenDisputes_ShouldCountSubmittedAndUnderReview() throws Exception {
        // Given
        Dispute dispute1 = new Dispute();
        dispute1.setStatus(DisputeStatus.SUBMITTED);
        
        Dispute dispute2 = new Dispute();
        dispute2.setStatus(DisputeStatus.SUBMITTED);
        
        Dispute dispute3 = new Dispute();
        dispute3.setStatus(DisputeStatus.UNDER_REVIEW);
        
        Dispute dispute4 = new Dispute();
        dispute4.setStatus(DisputeStatus.RESOLVED);

        given(userRepository.count()).willReturn(5L);
        given(itemRepository.count()).willReturn(10L);
        given(bookingRepository.count()).willReturn(15L);
        given(bookingRepository.findAll()).willReturn(Collections.emptyList());
        given(disputeRepository.findByStatus(DisputeStatus.SUBMITTED)).willReturn(Arrays.asList(dispute1, dispute2));
        given(disputeRepository.findByStatus(DisputeStatus.UNDER_REVIEW)).willReturn(Collections.singletonList(dispute3));

        // Expected open issues: 2 SUBMITTED + 1 UNDER_REVIEW = 3
        // When & Then
        mockMvc.perform(get("/api/admin/metrics")
                .session(adminSession)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAccounts").value(5))
                .andExpect(jsonPath("$.activeListings").value(10))
                .andExpect(jsonPath("$.totalBookings").value(15))
                .andExpect(jsonPath("$.openIssues").value(3));
    }

    // Helper method to create booking requests
    private BookingRequest createBooking(Long id, BookingStatus status, PaymentStatus paymentStatus, 
                                        Double totalPrice, LocalDateTime paidAt) {
        BookingRequest booking = new BookingRequest();
        booking.setId(id);
        booking.setStatus(status);
        booking.setPaymentStatus(paymentStatus);
        booking.setTotalPrice(totalPrice);
        booking.setPaidAt(paidAt);
        return booking;
    }
}
