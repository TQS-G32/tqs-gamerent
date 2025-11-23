package gamerent.data;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookingRepository extends JpaRepository<BookingRequest, Long> {
    List<BookingRequest> findByItemId(Long itemId);
    List<BookingRequest> findByUserId(Long userId);
    List<BookingRequest> findByItemIdAndStatus(Long itemId, BookingStatus status);
    
    // For finding overlapping bookings
    // Query: check if any booking for the same item overlaps with [start, end] and is APPROVED
    // We'll do logical checks in Service for simplicity or use @Query here.
    // Simple overlapping: (StartA <= EndB) and (EndA >= StartB)
}
