package gamerent.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {
    /**
     * Find chat by renter and item.
     */
    Optional<Chat> findByRenterIdAndItemId(Long renterId, Long itemId);

    /**
     * Find all chats where the user is either the renter or the owner.
     */
    @Query("SELECT c FROM Chat c WHERE c.renter.id = :userId OR c.owner.id = :userId ORDER BY c.updatedAt DESC")
    List<Chat> findByUserId(@Param("userId") Long userId);

    /**
     * Find all chats where the user is the owner.
     */
    List<Chat> findByOwnerIdOrderByUpdatedAtDesc(Long ownerId);

    /**
     * Find all chats where the user is the renter.
     */
    List<Chat> findByRenterIdOrderByUpdatedAtDesc(Long renterId);
}
