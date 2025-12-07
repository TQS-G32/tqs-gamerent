package gamerent.boundary;

import com.fasterxml.jackson.databind.ObjectMapper;
import gamerent.data.BookingRequest;
import gamerent.data.BookingStatus;
import gamerent.data.Item;
import gamerent.data.User;
import gamerent.data.UserRepository;
import gamerent.service.BookingService;
import gamerent.service.ItemService;
import gamerent.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
 
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {gamerent.boundary.AuthController.class, gamerent.boundary.ItemController.class, gamerent.boundary.BookingController.class})
@AutoConfigureMockMvc(addFilters = false)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BookingFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private ItemService itemService;

    @MockBean
    private BookingService bookingService;

    @Test
    void ownerListingAndBookingFlow() throws Exception {
        // Prepare users
        User owner = new User(); owner.setId(10L); owner.setEmail("ownerA@example.com"); owner.setName("ownerA"); owner.setRole("USER");
        User renter = new User(); renter.setId(20L); renter.setEmail("renterB@example.com"); renter.setName("renterB"); renter.setRole("USER");

        // Register owner -> registerUser returns owner with id
        when(userService.findByEmail("ownerA@example.com")).thenReturn(java.util.Optional.empty());
        when(userService.registerUser(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(owner.getId());
            u.setRole("USER");
            return u;
        });

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(Map.of("name","ownerA","email","ownerA@example.com","password","passA")))).andExpect(status().isOk());

        // Login owner - stub user lookup and password check
        when(userService.findByEmail("ownerA@example.com")).thenReturn(java.util.Optional.of(owner));
        when(userService.checkPassword(owner, "passA")).thenReturn(true);
        MockHttpSession sessionA = new MockHttpSession();
        mockMvc.perform(post("/api/auth/login").session(sessionA).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(Map.of("email","ownerA@example.com","password","passA"))))
                .andExpect(status().isOk());

        // Owner posts an item - mock userRepository and itemService
        Item created = new Item(); created.setId(100L); created.setName("Test Game"); created.setDescription("Nice game"); created.setPricePerDay(3.5);
        created.setOwner(owner);
        when(userRepository.findById(10L)).thenReturn(java.util.Optional.of(owner));
        when(itemService.addItem(any(Item.class), eq(owner))).thenReturn(created);

        String itemJson = objectMapper.writeValueAsString(Map.of("name","Test Game","description","Nice game","pricePerDay",3.5));
        String itemRes = mockMvc.perform(post("/api/items").session(sessionA).contentType(MediaType.APPLICATION_JSON).content(itemJson)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Item createdResp = objectMapper.readValue(itemRes, Item.class);
        assertThat(createdResp.getId()).isEqualTo(100L);

        // mock my-items
        when(itemService.getItemsByOwner(10L)).thenReturn(List.of(created));
        String myItems = mockMvc.perform(get("/api/items/my-items").session(sessionA)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(myItems).contains("Test Game");

        // Register and login renter
        when(userService.findByEmail("renterB@example.com")).thenReturn(java.util.Optional.empty());
        when(userService.registerUser(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(renter.getId());
            u.setRole("USER");
            return u;
        });
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(Map.of("name","renterB","email","renterB@example.com","password","passB")))).andExpect(status().isOk());

        when(userService.findByEmail("renterB@example.com")).thenReturn(java.util.Optional.of(renter));
        when(userService.checkPassword(renter, "passB")).thenReturn(true);
        MockHttpSession sessionB = new MockHttpSession();
        mockMvc.perform(post("/api/auth/login").session(sessionB).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(Map.of("email","renterB@example.com","password","passB")))).andExpect(status().isOk());

        // Renter requests booking - mock bookingService.createBooking
        BookingRequest booking = new BookingRequest();
        booking.setId(555L);
        booking.setItemId(100L);
        booking.setUserId(20L);
        booking.setStartDate(LocalDate.now().plusDays(1));
        booking.setEndDate(LocalDate.now().plusDays(3));
        booking.setStatus(BookingStatus.PENDING);
        when(bookingService.createBooking(100L, 20L, booking.getStartDate(), booking.getEndDate())).thenReturn(booking);

        Map<String,Object> bookingReq = Map.of("itemId", createdResp.getId(), "startDate", booking.getStartDate().toString(), "endDate", booking.getEndDate().toString());
        String bookingRes = mockMvc.perform(post("/api/bookings").session(sessionB).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(bookingReq))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Map<?,?> bookingMap = objectMapper.readValue(bookingRes, Map.class);
        Integer bookingId = (Integer) bookingMap.get("id");
        assertThat(bookingId).isNotNull();

        // Owner fetches incoming requests - mock bookingService.getOwnerBookings
        when(bookingService.getOwnerBookings(10L)).thenReturn(List.of(booking));
        String requests = mockMvc.perform(get("/api/bookings/requests").session(sessionA)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(requests).contains("PENDING");

        // Owner approves booking - mock updateStatus
        BookingRequest approved = booking;
        approved.setStatus(BookingStatus.APPROVED);
        when(bookingService.updateStatus(555L, BookingStatus.APPROVED, 10L)).thenReturn(approved);
        mockMvc.perform(put("/api/bookings/" + 555 + "/status").session(sessionA).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(Map.of("status","APPROVED"))))
                .andExpect(status().isOk());

        // Renter sees booking as APPROVED - mock getUserBookings
        when(bookingService.getUserBookings(20L)).thenReturn(List.of(approved));
        String renterBookings = mockMvc.perform(get("/api/bookings/my-bookings").session(sessionB)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(renterBookings).contains("APPROVED");
    }
}
