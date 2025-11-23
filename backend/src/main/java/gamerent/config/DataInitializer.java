package gamerent.config;

import gamerent.data.User;
import gamerent.data.UserRepository;
import gamerent.service.ItemService;
import gamerent.data.ItemRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    private final ItemRepository itemRepository;
    private final ItemService itemService;
    private final UserRepository userRepository;

    public DataInitializer(ItemRepository itemRepository, ItemService itemService, UserRepository userRepository) {
        this.itemRepository = itemRepository;
        this.itemService = itemService;
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        User demoUser;
        if (userRepository.count() == 0) {
            demoUser = new User();
            demoUser.setName("Demo User");
            demoUser.setEmail("demo@gamerent.com");
            demoUser.setPassword("password"); // Not used but good to have
            demoUser.setRole("OWNER");
            userRepository.save(demoUser);
        } else {
            demoUser = userRepository.findAll().get(0);
        }

        if (itemRepository.count() < 10) {
            itemService.populateFromIGDB(10, demoUser);
        }
    }
}
