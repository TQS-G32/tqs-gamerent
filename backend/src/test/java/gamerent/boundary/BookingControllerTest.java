package gamerent.boundary;

import gamerent.data.BookingRequest;
import gamerent.data.BookingStatus;
import gamerent.service.BookingService;
import gamerent.data.UserRepository;
import gamerent.data.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void createBooking_ShouldReturnCreated() throws Exception {
        // Mock User lookup (default ID 1)
        given(userRepository.findById(1L)).willReturn(Optional.of(new User()));
        
        BookingRequest booking = new BookingRequest();
        booking.setId(1L);
        booking.setItemId(1L);
        booking.setUserId(2L);
        booking.setStartDate(LocalDate.of(2025, 12, 1));
        booking.setEndDate(LocalDate.of(2025, 12, 5));
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalPrice(75.0);
        
        given(bookingService.createBooking(anyLong(), anyLong(), any(), any())).willReturn(booking);

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"itemId\": 1, \"userId\": 2, \"startDate\": \"2025-12-01\", \"endDate\": \"2025-12-05\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalPrice").value(75.0));
    }

    @Test
    void getMyBookings_ShouldReturnList() throws Exception {
        BookingRequest booking = new BookingRequest();
        booking.setId(1L);
        booking.setStatus(BookingStatus.PENDING);
        
        given(bookingService.getUserBookings(1L)).willReturn(List.of(booking));

        mockMvc.perform(get("/api/bookings/my-bookings"))
                .andExpect(status().isOk());
    }

    @Test
    void getBookingsByItem_ShouldReturnList() throws Exception {
        BookingRequest booking = new BookingRequest();
        booking.setId(1L);
        booking.setItemId(1L);
        
        given(bookingService.getItemBookings(1L)).willReturn(List.of(booking));

        mockMvc.perform(get("/api/bookings?itemId=1"))
                .andExpect(status().isOk());
    }

    @Test
    void getIncomingRequests_ShouldReturnOwnerBookings() throws Exception {
        BookingRequest booking = new BookingRequest();
        booking.setId(1L);
        booking.setStatus(BookingStatus.PENDING);
        
        given(bookingService.getOwnerBookings(1L)).willReturn(List.of(booking));

        mockMvc.perform(get("/api/bookings/requests"))
                .andExpect(status().isOk());
    }

    @Test
    void updateStatus_ShouldApproveBooking() throws Exception {
        BookingRequest booking = new BookingRequest();
        booking.setId(1L);
        booking.setStatus(BookingStatus.APPROVED);
        
        given(bookingService.updateStatus(1L, BookingStatus.APPROVED, 1L)).willReturn(booking);

        mockMvc.perform(put("/api/bookings/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }
}
