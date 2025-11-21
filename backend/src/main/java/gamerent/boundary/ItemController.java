package gamerent.boundary;

import gamerent.data.Item;
import gamerent.service.ItemService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/items")
@CrossOrigin(origins = "*")
public class ItemController {
    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping
    public List<Item> getAllItems() {
        return itemService.getAllItems();
    }

    @PostMapping
    public Item addItem(@RequestBody Item item) {
        return itemService.addItem(item);
    }
}
