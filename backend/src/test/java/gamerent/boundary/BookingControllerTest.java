package gamerent.boundary;

import gamerent.data.BookingRequest;
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

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        
        given(bookingService.createBooking(anyLong(), anyLong(), any(), any())).willReturn(new BookingRequest());

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"itemId\": 1, \"startDate\": \"2023-12-01\", \"endDate\": \"2023-12-05\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void getMyBookings_ShouldReturnList() throws Exception {
        given(bookingService.getUserBookings(1L)).willReturn(List.of(new BookingRequest()));

        mockMvc.perform(get("/api/bookings/my-bookings"))
                .andExpect(status().isOk());
    }
}
