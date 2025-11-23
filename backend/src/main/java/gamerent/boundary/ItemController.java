package gamerent.boundary;

import gamerent.data.Item;
import gamerent.data.User;
import gamerent.data.UserRepository;
import gamerent.service.ItemService;
import org.springframework.web.bind.annotation.*;
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
    public Item addItem(@RequestBody Item item) {
        // Default to user ID 1 (Demo User)
        User owner = userRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Default user not found. Ensure DataInitializer has run."));
        return itemService.addItem(item, owner);
    }
    
    @GetMapping("/my-items")
    public List<Item> getMyItems() {
        // Default to user ID 1
        return itemService.getItemsByOwner(1L);
    }
    
    @GetMapping("/{id}")
    public Item getItem(@PathVariable Long id) {
        return itemService.getItem(id);
    }
}
