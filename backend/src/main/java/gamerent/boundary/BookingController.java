package gamerent.boundary;

import gamerent.data.BookingRequest;
import gamerent.data.BookingStatus;
import gamerent.data.User;
import gamerent.data.UserRepository;
import gamerent.service.BookingService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*")
public class BookingController {
    private final BookingService bookingService;
    private final UserRepository userRepository;

    public BookingController(BookingService bookingService, UserRepository userRepository) {
        this.bookingService = bookingService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public BookingRequest createBooking(@RequestBody BookingRequest booking) {
        // Default to user ID 1
        User user = userRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Default user not found"));
        return bookingService.createBooking(booking.getItemId(), user.getId(), booking.getStartDate(), booking.getEndDate());
    }
    
    @GetMapping("/my-bookings")
    public List<BookingRequest> getMyBookings() {
        // Default to user ID 1
        return bookingService.getUserBookings(1L);
    }
    
    @GetMapping("/requests")
    public List<BookingRequest> getIncomingRequests() {
        // Default to user ID 1
        return bookingService.getOwnerBookings(1L);
    }
    
    @PutMapping("/{id}/status")
    public BookingRequest updateStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        // Default to user ID 1
        BookingStatus status = BookingStatus.valueOf(payload.get("status"));
        return bookingService.updateStatus(id, status, 1L);
    }
}
