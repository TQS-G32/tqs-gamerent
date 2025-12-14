package gamerent.service;

import gamerent.data.Dispute;
import gamerent.data.DisputeReason;
import gamerent.data.DisputeRepository;
import gamerent.data.DisputeStatus;
import gamerent.data.ItemRepository;
import gamerent.data.BookingRepository;
import gamerent.data.Item;
import gamerent.data.BookingRequest;
import gamerent.config.UnauthorizedException;
import gamerent.config.DisputeValidationException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class DisputeService {
    private static final Logger logger = Logger.getLogger(DisputeService.class.getName());
    private static final int MAX_DESCRIPTION_LENGTH = 500;
    private static final int DISPUTE_WINDOW_DAYS = 30;
    private static final String BOOKING_NOT_FOUND = "Booking not found!";
    private static final String ITEM_NOT_FOUND = "Item not found!";
    private static final String ADMIN = "ADMIN";

    private final DisputeRepository disputeRepository;
    private final BookingRepository bookingRepository;
    private final ItemRepository itemRepository;

    public DisputeService(DisputeRepository disputeRepository, 
                         BookingRepository bookingRepository,
                         ItemRepository itemRepository) {
        this.disputeRepository = disputeRepository;
        this.bookingRepository = bookingRepository;
        this.itemRepository = itemRepository;
    }

    public Dispute createDispute(Long userId, Long bookingId, DisputeReason reason, 
                                 String description, String evidenceUrls) {
        logger.log(Level.INFO, "Creating dispute - User: {0}, Booking: {1}, Reason: {2}", 
            new Object[]{userId, bookingId, reason});

        BookingRequest booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new NoSuchElementException(BOOKING_NOT_FOUND));
        
        validateUserInvolvement(userId, booking);
        validateBookingEligibility(booking);
        validateDisputeFields(reason, description);
        
        // Create and save dispute
        Dispute dispute = new Dispute(bookingId, userId, reason, description, evidenceUrls);
        Dispute saved = disputeRepository.save(dispute);
        
        notifyAdmin(saved);
        
        logger.log(Level.INFO, "Dispute created successfully - ID: {0}", saved.getId());
        return saved;
    }

    private void validateUserInvolvement(Long userId, BookingRequest booking) {
        Item item = itemRepository.findById(booking.getItemId())
            .orElseThrow(() -> new NoSuchElementException(ITEM_NOT_FOUND));
        
        boolean isRenter = booking.getUserId().equals(userId);
        boolean isOwner = item.getOwner() != null && item.getOwner().getId().equals(userId);
        
        if (!isRenter && !isOwner) {
            logger.log(Level.WARNING, "Unauthorized dispute attempt - User: {0}, Booking: {1}", 
                new Object[]{userId, booking.getId()});
            throw new UnauthorizedException("You are not involved in this booking");
        }
    }

    private void validateBookingEligibility(BookingRequest booking) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = booking.getEndDate();
        
        if (endDate == null) {
            throw new DisputeValidationException("Booking must have an end date");
        }
        
        // Check if booking is active (end date is today or in the future)
        boolean isActive = !endDate.isBefore(today);
        
        // Check if booking is recently completed (within last 30 days)
        boolean isRecentlyCompleted = endDate.isBefore(today) && 
            endDate.isAfter(today.minusDays(DISPUTE_WINDOW_DAYS));
        
        if (!isActive && !isRecentlyCompleted) {
            throw new DisputeValidationException(
                "Disputes can only be opened for active or recently completed bookings (within " + 
                DISPUTE_WINDOW_DAYS + " days)");
        }
    }

    private void validateDisputeFields(DisputeReason reason, String description) {
        if (reason == null) {
            throw new DisputeValidationException("Reason is required");
        }
        
        if (reason == DisputeReason.OTHER && (description == null || description.trim().isEmpty())) {
            throw new DisputeValidationException("Description is required for 'Other' reason");
        }
        
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new DisputeValidationException(
                "Description cannot exceed " + MAX_DESCRIPTION_LENGTH + " characters");
        }
    }

    private void notifyAdmin(Dispute dispute) {
        logger.log(Level.INFO, "ADMIN NOTIFICATION: New dispute submitted - ID: {0}, Booking: {1}, Reason: {2}", 
            new Object[]{dispute.getId(), dispute.getBookingId(), dispute.getReason()});
    }

    public Dispute updateDisputeStatus(Long disputeId, DisputeStatus newStatus, 
                                      String adminNotes, String adminRole) {
        if (!ADMIN.equals(adminRole)) {
            throw new UnauthorizedException("Only admins can update dispute status");
        }
        
        Dispute dispute = disputeRepository.findById(disputeId)
            .orElseThrow(() -> new NoSuchElementException("Dispute not found"));
        
        dispute.setStatus(newStatus);
        if (adminNotes != null) {
            dispute.setAdminNotes(adminNotes);
        }
        dispute.setUpdatedAt(LocalDateTime.now());
        
        logger.log(Level.INFO, "Dispute status updated - ID: {0}, New Status: {1}", 
            new Object[]{disputeId, newStatus});
        
        return disputeRepository.save(dispute);
    }

    public List<Dispute> getUserDisputes(Long userId, String userRole) {
        // If admin, return all disputes
        if (ADMIN.equals(userRole)) {
            return disputeRepository.findAll();
        }
        
        // Get all bookings where user is involved (as renter or owner)
        List<BookingRequest> userBookings = bookingRepository.findByUserId(userId);
        
        List<BookingRequest> ownerBookings = itemRepository.findByOwnerId(userId).stream()
            .flatMap(item -> bookingRepository.findByItemId(item.getId()).stream())
            .collect(Collectors.toList());
        
        List<Long> allBookingIds = userBookings.stream()
            .map(BookingRequest::getId)
            .collect(Collectors.toList());
        
        allBookingIds.addAll(ownerBookings.stream()
            .map(BookingRequest::getId)
            .collect(Collectors.toList()));
        
        // Get disputes where user is reporter or involved in booking
        return disputeRepository.findAll().stream()
            .filter(d -> d.getReporterId().equals(userId) || allBookingIds.contains(d.getBookingId()))
            .collect(Collectors.toList());
    }

    public Dispute getDisputeById(Long disputeId, Long userId, String userRole) {
        Dispute dispute = disputeRepository.findById(disputeId)
            .orElseThrow(() -> new NoSuchElementException("Dispute not found"));
        
        // If admin, allow access
        if (ADMIN.equals(userRole)) {
            return dispute;
        }
        
        // Verify user is involved in the booking
        BookingRequest booking = bookingRepository.findById(dispute.getBookingId())
            .orElseThrow(() -> new NoSuchElementException(BOOKING_NOT_FOUND));
        
        Item item = itemRepository.findById(booking.getItemId())
            .orElseThrow(() -> new NoSuchElementException(ITEM_NOT_FOUND));
        
        boolean isRenter = booking.getUserId().equals(userId);
        boolean isOwner = item.getOwner() != null && item.getOwner().getId().equals(userId);
        boolean isReporter = dispute.getReporterId().equals(userId);
        
        if (!isRenter && !isOwner && !isReporter) {
            throw new UnauthorizedException("You do not have permission to view this dispute");
        }
        
        return dispute;
    }

    public List<Dispute> getDisputesByBooking(Long bookingId, Long userId, String userRole) {
        // Verify user has access to this booking
        BookingRequest booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new NoSuchElementException(BOOKING_NOT_FOUND));
        
        if (!ADMIN.equals(userRole)) {
            Item item = itemRepository.findById(booking.getItemId())
                .orElseThrow(() -> new NoSuchElementException(ITEM_NOT_FOUND));
            
            boolean isRenter = booking.getUserId().equals(userId);
            boolean isOwner = item.getOwner() != null && item.getOwner().getId().equals(userId);
            
            if (!isRenter && !isOwner) {
                throw new UnauthorizedException("You do not have permission to view disputes for this booking");
            }
        }
        
        return disputeRepository.findByBookingId(bookingId);
    }
}
