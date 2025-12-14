package gamerent.boundary;

import gamerent.data.Item;
import gamerent.data.User;
import gamerent.data.UserRepository;
import gamerent.service.ItemService;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/items")
public class ItemController {
    private final ItemService itemService;
    private final UserRepository userRepository;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final String USER_ID_KEY = "userId";

    public ItemController(ItemService itemService, UserRepository userRepository) {
        this.itemService = itemService;
        this.userRepository = userRepository;
    }

    @GetMapping("/catalog")
    public Map<String, Object> getCatalog(@RequestParam(required=false) String q, 
        @RequestParam(required=false) String category,
        @RequestParam(required=false) Boolean rentable,
        @RequestParam(defaultValue = "0") int page) {
        // If rentable filter requested, get full results and filter before paginating
        List<Item> allResults = itemService.searchAllItemsByNameAndCategory(q, category);
        if (rentable != null && rentable) {
            allResults = allResults.stream().filter(i -> i.getAvailable() != null && i.getAvailable() && i.getPricePerDay() != null).toList();
        }
        int totalCount = allResults.size();
        int totalPages = (totalCount + DEFAULT_PAGE_SIZE - 1) / DEFAULT_PAGE_SIZE;
        int start = page * DEFAULT_PAGE_SIZE;
        int end = Math.min(start + DEFAULT_PAGE_SIZE, totalCount);
        List<Item> items = start >= totalCount ? List.of() : allResults.subList(start, end);
        
        return Map.of(
            "items", items,
            "page", page,
            "pageSize", DEFAULT_PAGE_SIZE,
            "totalCount", totalCount,
            "totalPages", totalPages
        );
    }

    @GetMapping
    public Map<String, Object> getAllItems(@RequestParam(required=false) Boolean rentable,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int pageSize,
                                           @RequestParam(required = false) Long ownerId) {
        if (ownerId != null) {
            int safePageSize = pageSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, 50);
            List<Item> items = itemService.getItemsByOwnerPaginated(ownerId, page, safePageSize);
            int totalCount = itemService.getItemsByOwnerCount(ownerId);
            int totalPages = (totalCount + safePageSize - 1) / safePageSize;
            return Map.of(
                    "items", items,
                    "page", page,
                    "pageSize", safePageSize,
                    "totalCount", totalCount,
                    "totalPages", totalPages
            );
        }
        List<Item> allItems = itemService.getAllItems();
        if (rentable != null && rentable) {
            allItems = allItems.stream().filter(i -> i.getAvailable() != null && i.getAvailable() && i.getPricePerDay() != null).toList();
        }
        int safePageSize = pageSize <= 0 ? DEFAULT_PAGE_SIZE : pageSize;
        int totalCount = allItems.size();
        int totalPages = (totalCount + safePageSize - 1) / safePageSize;
        int start = page * safePageSize;
        int end = Math.min(start + safePageSize, totalCount);
        List<Item> items = start >= totalCount ? List.of() : allItems.subList(start, end);
        
        return Map.of(
            "items", items,
            "page", page,
            "pageSize", safePageSize,
            "totalCount", totalCount,
            "totalPages", totalPages
        );
    }
    
    @GetMapping("/search")
    public Map<String, Object> search(
        @RequestParam(required=false) String q, 
        @RequestParam(required=false) String category,
        @RequestParam(required=false) Boolean rentable,
        @RequestParam(defaultValue = "0") int page) {
        List<Item> allResults = itemService.searchItems(q, category);
        if (rentable != null && rentable) {
            allResults = allResults.stream().filter(i -> i.getAvailable() != null && i.getAvailable() && i.getPricePerDay() != null).toList();
        }
        int totalCount = allResults.size();
        int totalPages = (totalCount + DEFAULT_PAGE_SIZE - 1) / DEFAULT_PAGE_SIZE;
        int start = page * DEFAULT_PAGE_SIZE;
        int end = Math.min(start + DEFAULT_PAGE_SIZE, totalCount);
        List<Item> items = start >= totalCount ? List.of() : allResults.subList(start, end);
        
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
        Long ownerId = resolveOwnerId(request, null, true);

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("Owner not found. Ensure DataInitializer has run."));
        return itemService.addItem(item, owner);
    }
    
    @GetMapping("/my-items")
    public List<Item> getMyItems(HttpServletRequest request) {
        Long ownerId = resolveOwnerId(request, null, true);
        return itemService.getItemsByOwner(ownerId);
    }
    
    @GetMapping("/{id}")
    public Item getItem(@PathVariable Long id) {
        return itemService.getItem(id);
    }

    @PutMapping("/{id}/settings")
    public Map<String, Object> updateItemSettings(@PathVariable Long id, @RequestBody Map<String, Object> payload, HttpServletRequest request) {
        Long ownerId = resolveOwnerId(request, 1L, false);

        Boolean available = payload.containsKey("available") ? (Boolean) payload.get("available") : null;
        Integer minRentalDays = payload.containsKey("minRentalDays") ? (Integer) payload.get("minRentalDays") : null;

        try {
            Item updated = itemService.updateItemSettings(id, ownerId, available, minRentalDays);
            return Map.of("message", "Settings updated successfully", "item", updated);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private Long resolveOwnerId(HttpServletRequest request, Long defaultValue, boolean require) {
        Object uid = request.getSession(false) != null ? request.getSession(false).getAttribute(USER_ID_KEY) : null;
        if (uid == null) {
            if (require) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
            return defaultValue;
        }
        if (uid instanceof Long longValue) return longValue;
        if (uid instanceof Integer intValue) return intValue.longValue();
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
    }
}
