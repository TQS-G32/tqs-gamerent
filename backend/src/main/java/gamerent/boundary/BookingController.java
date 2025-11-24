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
        return bookingService.createBooking(booking.getItemId(), booking.getUserId(), booking.getStartDate(), booking.getEndDate());
    }
    
    @GetMapping("/my-bookings")
    public List<BookingRequest> getMyBookings(@RequestParam(required = false) Long userId) {
        Long resolvedUserId = userId != null ? userId : 1L;
        return bookingService.getUserBookings(resolvedUserId);
    }
    
    @GetMapping("/requests")
    public List<BookingRequest> getIncomingRequests(@RequestParam(required = false) Long ownerId) {
        Long resolvedOwnerId = ownerId != null ? ownerId : 1L;
        return bookingService.getOwnerBookings(resolvedOwnerId);
    }
    
    @PutMapping("/{id}/status")
    public BookingRequest updateStatus(@PathVariable Long id, @RequestBody Map<String, String> payload, @RequestParam(required = false) Long ownerId) {
        BookingStatus status = BookingStatus.valueOf(payload.get("status"));
        Long resolvedOwnerId = ownerId != null ? ownerId : 1L;
        return bookingService.updateStatus(id, status, resolvedOwnerId);
    }
}

