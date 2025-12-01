package gamerent.boundary;

import gamerent.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
class BookingControllerIT {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private BookingRepository bookingRepository;
    
    @Autowired
    private ItemRepository itemRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    private User owner;
    private User renter;
    private Item item;
    
    @BeforeEach
    void setUp() {
        // Clean up test data if it exists from previous runs
        cleanupUser("owner@test.com");
        cleanupUser("renter@test.com");
        
        // Create owner
        owner = new User();
        owner.setName("Test Owner");
        owner.setEmail("owner@test.com");
        owner.setPassword("password");
        owner.setRole("OWNER");
        owner = userRepository.save(owner);
        
        // Create renter
        renter = new User();
        renter.setName("Test Renter");
        renter.setEmail("renter@test.com");
        renter.setPassword("password");
        renter.setRole("RENTER");
        renter = userRepository.save(renter);
        
        // Create item
        item = new Item();
        item.setName("Test Console");
        item.setDescription("A test console");
        item.setCategory("Console");
        item.setPricePerDay(20.0);
        item.setOwner(owner);
        item = itemRepository.save(item);
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
    void createBooking_ShouldReturn200AndCreateBooking() throws Exception {
        String json = """
            {
                "itemId": %d,
                "userId": %d,
                "startDate": "2025-12-01",
                "endDate": "2025-12-05"
            }
            """.formatted(item.getId(), renter.getId());
        
        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.itemId").value(item.getId()))
                .andExpect(jsonPath("$.userId").value(renter.getId()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }
    
    @Test
    void getMyBookings_ShouldReturn200AndListBookings() throws Exception {
        // Create a booking
        BookingRequest booking = new BookingRequest();
        booking.setItemId(item.getId());
        booking.setUserId(renter.getId());
        booking.setStartDate(LocalDate.of(2025, 12, 1));
        booking.setEndDate(LocalDate.of(2025, 12, 5));
        booking.setStatus(BookingStatus.PENDING);
        bookingRepository.save(booking);
        
        mockMvc.perform(get("/api/bookings/my-bookings")
                .param("userId", renter.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(renter.getId()))
                .andExpect(jsonPath("$[0].itemId").value(item.getId()));
    }
    
    @Test
    void getIncomingRequests_ShouldReturn200AndListRequests() throws Exception {
        // Create a booking for owner's item
        BookingRequest booking = new BookingRequest();
        booking.setItemId(item.getId());
        booking.setUserId(renter.getId());
        booking.setStartDate(LocalDate.of(2025, 12, 1));
        booking.setEndDate(LocalDate.of(2025, 12, 5));
        booking.setStatus(BookingStatus.PENDING);
        bookingRepository.save(booking);
        
        mockMvc.perform(get("/api/bookings/requests")
                .param("ownerId", owner.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].itemId").value(item.getId()))
                .andExpect(jsonPath("$[0].userId").value(renter.getId()));
    }
    
    @Test
    void updateStatus_ShouldReturn200AndUpdateBooking() throws Exception {
        // Create a booking
        BookingRequest booking = new BookingRequest();
        booking.setItemId(item.getId());
        booking.setUserId(renter.getId());
        booking.setStartDate(LocalDate.of(2025, 12, 1));
        booking.setEndDate(LocalDate.of(2025, 12, 5));
        booking.setStatus(BookingStatus.PENDING);
        booking = bookingRepository.save(booking);
        
        String json = """
            {
                "status": "APPROVED"
            }
            """;
        
        mockMvc.perform(put("/api/bookings/{id}/status", booking.getId())
                .param("ownerId", owner.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }
}
