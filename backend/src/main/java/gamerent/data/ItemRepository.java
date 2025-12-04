package gamerent.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {
    List<Item> findByNameContainingIgnoreCase(String name);
    List<Item> findByCategoryIgnoreCase(String category);
    List<Item> findByNameContainingIgnoreCaseAndCategoryIgnoreCase(String name, String category);
    List<Item> findByOwnerId(Long ownerId);
    
    // Fuzzy search using SQL LIKE with wildcards for partial matching
    @Query("SELECT i FROM Item i WHERE LOWER(i.name) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY i.name ASC")
    List<Item> fuzzySearchByName(@Param("query") String query);
    
    @Query("SELECT i FROM Item i WHERE LOWER(i.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "AND LOWER(i.category) LIKE LOWER(CONCAT('%', :category, '%')) ORDER BY i.name ASC")
    List<Item> fuzzySearchByNameAndCategory(@Param("query") String query, @Param("category") String category);
}
