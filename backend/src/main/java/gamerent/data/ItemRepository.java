package gamerent.data;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {
    List<Item> findByNameContainingIgnoreCase(String name);
    List<Item> findByCategoryIgnoreCase(String category);
    List<Item> findByNameContainingIgnoreCaseAndCategoryIgnoreCase(String name, String category);
    List<Item> findByOwnerId(Long ownerId);
}
