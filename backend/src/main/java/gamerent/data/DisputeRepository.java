package gamerent.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long> {
    
    List<Dispute> findByBookingId(Long bookingId);
    
    List<Dispute> findByReporterId(Long reporterId);
    
    List<Dispute> findByStatus(DisputeStatus status);
    
    List<Dispute> findByReporterIdOrBookingIdIn(Long reporterId, List<Long> bookingIds);
}
