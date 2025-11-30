package gamerent.config;

import gamerent.data.User;
import gamerent.data.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import gamerent.service.ItemService;
import gamerent.data.ItemRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    private final ItemRepository itemRepository;
    private final ItemService itemService;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public DataInitializer(ItemRepository itemRepository, ItemService itemService, UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.itemRepository = itemRepository;
        this.itemService = itemService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        User demoUser;
        if (userRepository.count() == 0) {
            // Create two default users: a regular user and an admin
            User demo = new User();
            demo.setName("Demo User");
            demo.setEmail("demo@gamerent.com");
            demo.setPassword(passwordEncoder.encode("password"));
            demo.setRole("USER");
            userRepository.save(demo);

            User admin = new User();
            admin.setName("Admin");
            admin.setEmail("admin@gamerent.com");
            admin.setPassword(passwordEncoder.encode("adminpass"));
            admin.setRole("ADMIN");
            userRepository.save(admin);

            demoUser = demo;
        } else {
            demoUser = userRepository.findAll().get(0);
        }

        if (itemRepository.count() < 10) {
            itemService.populateFromIGDB(10, demoUser);
        }
    }
}
