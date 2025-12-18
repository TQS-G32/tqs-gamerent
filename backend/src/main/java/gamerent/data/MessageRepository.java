package gamerent.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    /**
     * Find all messages in a chat ordered by sent time.
     */
    List<Message> findByChatIdOrderBySentAtAsc(Long chatId);

    /**
     * Count unread messages in a chat for a specific user.
     */
    Long countByChatIdAndIsReadFalseAndSenderIdNot(Long chatId, Long userId);
}
