package gamerent.service;

import gamerent.data.BookingRequest;
import gamerent.data.BookingRepository;
import gamerent.data.BookingStatus;
import gamerent.data.Item;
import gamerent.data.ItemRepository;
import gamerent.data.PaymentStatus;
import gamerent.config.BookingValidationException;
import gamerent.config.UnauthorizedException;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.NoSuchElementException;

@Service
public class BookingService {
    private static final Logger logger = Logger.getLogger(BookingService.class.getName());
    
    private final BookingRepository bookingRepository;
    private final ItemRepository itemRepository;

    public BookingService(BookingRepository bookingRepository, ItemRepository itemRepository) {
        this.bookingRepository = bookingRepository;
        this.itemRepository = itemRepository;
    }

    public BookingRequest createBooking(Long itemId, Long userId, LocalDate start, LocalDate end) {
        logger.log(Level.INFO, "Creating booking - Item: {0}, User: {1}, Period: {2} to {3}", 
            new Object[]{itemId, userId, start, end});
        
        Item item = itemRepository.findById(itemId)
            .orElseThrow(() -> {
                logger.log(Level.WARNING, "Booking creation failed - Item not found: {0}", itemId);
                return new NoSuchElementException("Item not found");
            });

        validateOwnership(item, userId);
        validateItemAvailability(item);
        validateDates(start, end);
        validateDateRange(start, end, itemId);
        validateMinimalRentalPeriod(start, end, item);

        BookingRequest created = createAndSaveBooking(itemId, userId, start, end, item);
        logger.log(Level.INFO, "Booking created successfully - ID: {0}, Price: ${1}", 
            new Object[]{created.getId(), created.getTotalPrice()});
        return created;
    }

    private void validateOwnership(Item item, Long userId) {
        if (item.getOwner().getId().equals(userId)) {
            logger.log(Level.WARNING, "User {0} attempted to rent their own item {1}", 
                new Object[]{userId, item.getId()});
            throw new BookingValidationException("You cannot rent your own item");
        }
    }

    private void validateItemAvailability(Item item) {
        if (item.getAvailable() == null || !item.getAvailable()) {
            logger.log(Level.WARNING, "Booking attempt on unavailable item: {0}", item.getId());
            throw new BookingValidationException("Item is currently not available for rent");
        }
    }

    private void validateDates(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new BookingValidationException("Start and end dates required");
        }
        if (start.isAfter(end)) {
            throw new BookingValidationException("Start date must be before end date");
        }
        if (start.isBefore(LocalDate.now()) || end.isBefore(LocalDate.now())) {
            throw new BookingValidationException("Start and End date must be in the future");
        }
    }

    private void validateDateRange(LocalDate start, LocalDate end, Long itemId) {
        List<BookingRequest> existing = bookingRepository.findByItemIdAndStatus(itemId, BookingStatus.APPROVED);
        for (BookingRequest b : existing) {
            if (isOverlapping(start, end, b.getStartDate(), b.getEndDate())) {
                throw new BookingValidationException("Item is not available for these dates");
            }
        }
    }

    private void validateMinimalRentalPeriod(LocalDate start, LocalDate end, Item item) {
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        if (days <= 0) days = 1;
        Integer minDays = item.getMinRentalDays() == null ? 1 : item.getMinRentalDays();
        if (minDays != null && days < minDays) {
            throw new BookingValidationException("Minimum rental period is " + minDays + " day(s)");
        }
    }

    private BookingRequest createAndSaveBooking(Long itemId, Long userId, LocalDate start, LocalDate end, Item item) {
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        if (days <= 0) days = 1;

        BookingRequest request = new BookingRequest();
        request.setItemId(itemId);
        request.setUserId(userId);
        request.setStartDate(start);
        request.setEndDate(end);
        request.setStatus(BookingStatus.PENDING);
        Double total = item.getPricePerDay() != null ? item.getPricePerDay() * days : 0.0;
        request.setTotalPrice(Math.round(total * 100.0) / 100.0);
        
        return bookingRepository.save(request);
    }
    
    public BookingRequest updateStatus(Long bookingId, BookingStatus status, Long ownerId) {
        logger.log(Level.INFO, "Updating booking status - Booking: {0}, Status: {1}, Owner: {2}", 
            new Object[]{bookingId, status, ownerId});
        
        BookingRequest booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> {
                logger.log(Level.WARNING, "Status update failed - Booking not found: {0}", bookingId);
                return new NoSuchElementException("Booking not found");
            });
            
        Item item = itemRepository.findById(booking.getItemId())
            .orElseThrow(() -> new NoSuchElementException("Item not found"));
            
        if (!item.getOwner().getId().equals(ownerId)) {
            throw new UnauthorizedException("Unauthorized: You are not the owner of this item");
        }
        
        // If transitioning to APPROVED for the first time, store approval + payment deadline
        if (status == BookingStatus.APPROVED && booking.getStatus() != BookingStatus.APPROVED) {
            LocalDateTime approvedAt = LocalDateTime.now();
            booking.setApprovedAt(approvedAt);
            booking.setPaymentDueAt(computePaymentDueAt(approvedAt));
        }

        booking.setStatus(status);
        return bookingRepository.save(booking);
    }

    /**
     * Payment window: until end of approval day, except if approval happens too close to midnight
     * (less than 60 minutes remaining), then extend to end of next day.
     */
    private LocalDateTime computePaymentDueAt(LocalDateTime approvedAt) {
        LocalDateTime endOfToday = approvedAt.toLocalDate().atTime(LocalTime.MAX);
        long minutesRemaining = java.time.Duration.between(approvedAt, endOfToday).toMinutes();
        if (minutesRemaining < 60) {
            return approvedAt.toLocalDate().plusDays(1).atTime(LocalTime.MAX);
        }
        return endOfToday;
    }
    
    public List<BookingRequest> getUserBookings(Long userId) {
        List<BookingRequest> list = bookingRepository.findByUserId(userId);
        expireUnpaidApprovedBookings(list);
        return list;
    }
    
    public List<BookingRequest> getItemBookings(Long itemId) {
        List<BookingRequest> list = bookingRepository.findByItemId(itemId);
        expireUnpaidApprovedBookings(list);
        return list;
    }
    
    // Get all bookings for items owned by ownerId
    public List<BookingRequest> getOwnerBookings(Long ownerId) {
        List<Item> ownerItems = itemRepository.findByOwnerId(ownerId);
        
        List<BookingRequest> list = ownerItems.stream()
            .flatMap(item -> bookingRepository.findByItemId(item.getId()).stream())
            .toList();
        expireUnpaidApprovedBookings(list);
        return list;
    }

    /**
     * Auto-expire payment windows on read: if an APPROVED booking is still UNPAID and paymentDueAt is in the past,
     * flip it to CANCELLED so it doesn't linger forever.
     */
    private void expireUnpaidApprovedBookings(List<BookingRequest> bookings) {
        if (bookings == null || bookings.isEmpty()) return;
        LocalDateTime now = LocalDateTime.now();
        for (BookingRequest b : bookings) {
            if ((b == null) || (b.getStatus() != BookingStatus.APPROVED) || (b.getPaymentStatus() == PaymentStatus.PAID))
                continue;
            LocalDateTime dueAt = b.getPaymentDueAt();
            if (dueAt != null && now.isAfter(dueAt)) {
                b.setStatus(BookingStatus.CANCELLED);
                bookingRepository.save(b);
            }
        }
    }

    private boolean isOverlapping(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return !start1.isAfter(end2) && !start2.isAfter(end1);
    }
}
