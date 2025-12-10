package gamerent.data;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByBookingId(Long bookingId);
    List<Review> findByTargetTypeAndTargetId(ReviewTargetType targetType, Long targetId);
    Optional<Review> findByBookingIdAndReviewerIdAndTargetTypeAndTargetId(Long bookingId, Long reviewerId, ReviewTargetType targetType, Long targetId);
}


