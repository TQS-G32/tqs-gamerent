package gamerent.boundary;

import gamerent.data.BookingRequest;
import gamerent.data.BookingStatus;
import gamerent.data.User;
import gamerent.data.UserRepository;
import gamerent.service.BookingService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*")
public class BookingController {
    private static final String USER_ID_KEY = "userId";
    private final BookingService bookingService;
    private final UserRepository userRepository;

    public BookingController(BookingService bookingService, UserRepository userRepository) {
        this.bookingService = bookingService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public BookingRequest createBooking(@RequestBody BookingRequest booking, HttpServletRequest request) {
        // Resolve current user from session if present
        Long userId = booking.getUserId();
        Object uid = request.getSession(false) != null ? request.getSession(false).getAttribute(USER_ID_KEY) : null;
        if (uid instanceof Long) userId = (Long) uid;
        else if (uid instanceof Integer) userId = ((Integer) uid).longValue();
        if (userId == null) userId = booking.getUserId();

        try {
            return bookingService.createBooking(booking.getItemId(), userId, booking.getStartDate(), booking.getEndDate());
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
    
    @GetMapping
    public List<BookingRequest> getBookingsByItem(@RequestParam(required = false) Long itemId) {
        if (itemId != null && itemId > 0) {
            return bookingService.getItemBookings(itemId);
        }
        return List.of();
    }
    
    @GetMapping("/my-bookings")
    public List<BookingRequest> getMyBookings(@RequestParam(required = false) Long userId, HttpServletRequest request) {
        Long resolvedUserId = userId;
        Object uid = request.getSession(false) != null ? request.getSession(false).getAttribute(USER_ID_KEY) : null;
        if (uid instanceof Long) resolvedUserId = (Long) uid;
        else if (uid instanceof Integer) resolvedUserId = ((Integer) uid).longValue();
        if (resolvedUserId == null) resolvedUserId = 1L;
        return bookingService.getUserBookings(resolvedUserId);
    }
    
    @GetMapping("/requests")
    public List<BookingRequest> getIncomingRequests(@RequestParam(required = false) Long ownerId, HttpServletRequest request) {
        Long resolvedOwnerId = ownerId;
        Object uid = request.getSession(false) != null ? request.getSession(false).getAttribute(USER_ID_KEY) : null;
        if (uid instanceof Long) resolvedOwnerId = (Long) uid;
        else if (uid instanceof Integer) resolvedOwnerId = ((Integer) uid).longValue();
        if (resolvedOwnerId == null) resolvedOwnerId = 1L;
        return bookingService.getOwnerBookings(resolvedOwnerId);
    }
    
    @PutMapping("/{id}/status")
    public BookingRequest updateStatus(@PathVariable Long id, @RequestBody Map<String, String> payload, @RequestParam(required = false) Long ownerId, HttpServletRequest request) {
        BookingStatus status = BookingStatus.valueOf(payload.get("status"));
        Long resolvedOwnerId = ownerId;
        Object uid = request.getSession(false) != null ? request.getSession(false).getAttribute(USER_ID_KEY) : null;
        if (uid instanceof Long) resolvedOwnerId = (Long) uid;
        else if (uid instanceof Integer) resolvedOwnerId = ((Integer) uid).longValue();
        if (resolvedOwnerId == null) resolvedOwnerId = 1L;
        try {
            return bookingService.updateStatus(id, status, resolvedOwnerId);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}

