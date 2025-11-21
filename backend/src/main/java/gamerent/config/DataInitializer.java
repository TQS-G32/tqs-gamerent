package gamerent.config;

import gamerent.service.ItemService;
import gamerent.data.ItemRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    private final ItemRepository itemRepository;
    private final ItemService itemService;

    public DataInitializer(ItemRepository itemRepository, ItemService itemService) {
        this.itemRepository = itemRepository;
        this.itemService = itemService;
    }

    @Override
    public void run(String... args) {
        if (itemRepository.count() < 10) {
            itemService.populateFromIGDB(10);
        }
    }
}