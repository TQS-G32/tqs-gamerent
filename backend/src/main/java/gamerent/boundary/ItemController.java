package gamerent.boundary;

import gamerent.data.Item;
import gamerent.data.User;
import gamerent.data.UserRepository;
import gamerent.service.ItemService;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/items")
@CrossOrigin(origins = "*")
public class ItemController {
    private final ItemService itemService;
    private final UserRepository userRepository;
    private static final int DEFAULT_PAGE_SIZE = 10;

    public ItemController(ItemService itemService, UserRepository userRepository) {
        this.itemService = itemService;
        this.userRepository = userRepository;
    }

    @GetMapping("/catalog")
    public Map<String, Object> getCatalog(@RequestParam(required=false) String q, 
            @RequestParam(required=false) String category,
            @RequestParam(defaultValue = "0") int page) {
        List<Item> items = itemService.searchAllItemsPaginated(q, category, page, DEFAULT_PAGE_SIZE);
        int totalCount = itemService.getSearchAllItemsResultCount(q, category);
        int totalPages = (totalCount + DEFAULT_PAGE_SIZE - 1) / DEFAULT_PAGE_SIZE;
        
        return Map.of(
            "items", items,
            "page", page,
            "pageSize", DEFAULT_PAGE_SIZE,
            "totalCount", totalCount,
            "totalPages", totalPages
        );
    }

    @GetMapping
    public Map<String, Object> getAllItems(@RequestParam(defaultValue = "0") int page) {
        List<Item> items = itemService.getAllItemsPaginated(page, DEFAULT_PAGE_SIZE);
        int totalCount = itemService.getTotalItemCount();
        int totalPages = (totalCount + DEFAULT_PAGE_SIZE - 1) / DEFAULT_PAGE_SIZE;
        
        return Map.of(
            "items", items,
            "page", page,
            "pageSize", DEFAULT_PAGE_SIZE,
            "totalCount", totalCount,
            "totalPages", totalPages
        );
    }
    
    @GetMapping("/search")
    public Map<String, Object> search(
            @RequestParam(required=false) String q, 
            @RequestParam(required=false) String category,
            @RequestParam(defaultValue = "0") int page) {
        List<Item> items = itemService.searchItemsPaginated(q, category, page, DEFAULT_PAGE_SIZE);
        int totalCount = itemService.getSearchResultCount(q, category);
        int totalPages = (totalCount + DEFAULT_PAGE_SIZE - 1) / DEFAULT_PAGE_SIZE;
        
        return Map.of(
            "items", items,
            "page", page,
            "pageSize", DEFAULT_PAGE_SIZE,
            "totalCount", totalCount,
            "totalPages", totalPages
        );
    }

    @PostMapping
    public Item addItem(@RequestBody Item item, HttpServletRequest request) {
        // Resolve current user from session if present
        Long ownerId = 1L;
        Object uid = request.getSession(false) != null ? request.getSession(false).getAttribute("userId") : null;
        if (uid instanceof Long) ownerId = (Long) uid;
        else if (uid instanceof Integer) ownerId = ((Integer) uid).longValue();

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("Owner not found. Ensure DataInitializer has run."));
        return itemService.addItem(item, owner);
    }
    
    @GetMapping("/my-items")
    public List<Item> getMyItems(HttpServletRequest request) {
        Long ownerId = 1L;
        Object uid = request.getSession(false) != null ? request.getSession(false).getAttribute("userId") : null;
        if (uid instanceof Long) ownerId = (Long) uid;
        else if (uid instanceof Integer) ownerId = ((Integer) uid).longValue();

        return itemService.getItemsByOwner(ownerId);
    }
    
    @GetMapping("/{id}")
    public Item getItem(@PathVariable Long id) {
        return itemService.getItem(id);
    }
}
