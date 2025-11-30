package gamerent.boundary;

import gamerent.data.Item;
import gamerent.data.User;
import gamerent.data.UserRepository;
import gamerent.service.ItemService;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/items")
@CrossOrigin(origins = "*")
public class ItemController {
    private final ItemService itemService;
    private final UserRepository userRepository;

    public ItemController(ItemService itemService, UserRepository userRepository) {
        this.itemService = itemService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<Item> getAllItems() {
        return itemService.getAllItems();
    }
    
    @GetMapping("/search")
    public List<Item> search(@RequestParam(required=false) String q, @RequestParam(required=false) String category) {
        return itemService.searchItems(q, category);
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
