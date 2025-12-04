package gamerent.service;

import gamerent.data.BookingRequest;
import gamerent.data.BookingRepository;
import gamerent.data.BookingStatus;
import gamerent.data.Item;
import gamerent.data.ItemRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingService {
    
    private final BookingRepository bookingRepository;
    private final ItemRepository itemRepository;

    public BookingService(BookingRepository bookingRepository, ItemRepository itemRepository) {
        this.bookingRepository = bookingRepository;
        this.itemRepository = itemRepository;
    }

    public BookingRequest createBooking(Long itemId, Long userId, LocalDate start, LocalDate end) {
        Item item = itemRepository.findById(itemId)
            .orElseThrow(() -> new RuntimeException("Item not found"));

        if (item.getOwner().getId().equals(userId)) {
            throw new RuntimeException("You cannot rent your own item");
        }
        // Validate dates
        if (start == null || end == null) throw new RuntimeException("Start and end dates required");
        if (start.isAfter(end)) throw new RuntimeException("Start date must be before end date");

        // Check overlapping approved bookings
        List<BookingRequest> existing = bookingRepository.findByItemIdAndStatus(itemId, BookingStatus.APPROVED);
        for (BookingRequest b : existing) {
            if (isOverlapping(start, end, b.getStartDate(), b.getEndDate())) {
                throw new RuntimeException("Item is not available for these dates");
            }
        }
        
        BookingRequest request = new BookingRequest();
        request.setItemId(itemId);
        request.setUserId(userId);
        request.setStartDate(start);
        request.setEndDate(end);
        request.setStatus(BookingStatus.PENDING);
        // Calculate total price (days inclusive)
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        if (days <= 0) days = 1;
        Double total = item.getPricePerDay() != null ? item.getPricePerDay() * (double) days : 0.0;
        request.setTotalPrice(Math.round(total * 100.0) / 100.0);
        
        return bookingRepository.save(request);
    }
    
    public BookingRequest updateStatus(Long bookingId, BookingStatus status, Long ownerId) {
        BookingRequest booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));
            
        Item item = itemRepository.findById(booking.getItemId())
            .orElseThrow(() -> new RuntimeException("Item not found"));
            
        if (!item.getOwner().getId().equals(ownerId)) {
            throw new RuntimeException("Unauthorized: You are not the owner of this item");
        }
        
        booking.setStatus(status);
        return bookingRepository.save(booking);
    }
    
    public List<BookingRequest> getUserBookings(Long userId) {
        return bookingRepository.findByUserId(userId);
    }
    
    public List<BookingRequest> getItemBookings(Long itemId) {
        return bookingRepository.findByItemId(itemId);
    }
    
    // Get all bookings for items owned by ownerId
    public List<BookingRequest> getOwnerBookings(Long ownerId) {
        List<Item> ownerItems = itemRepository.findByOwnerId(ownerId);
        List<Long> itemIds = ownerItems.stream().map(Item::getId).collect(Collectors.toList());
        
        return ownerItems.stream()
            .flatMap(item -> bookingRepository.findByItemId(item.getId()).stream())
            .collect(Collectors.toList());
    }

    private boolean isOverlapping(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return !start1.isAfter(end2) && !start2.isAfter(end1);
    }
}
