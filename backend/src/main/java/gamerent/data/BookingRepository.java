package gamerent.data;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookingRepository extends JpaRepository<BookingRequest, Long> {
    List<BookingRequest> findByItemId(Long itemId);
    List<BookingRequest> findByUserId(Long userId);
    List<BookingRequest> findByItemIdAndStatus(Long itemId, BookingStatus status);
}
