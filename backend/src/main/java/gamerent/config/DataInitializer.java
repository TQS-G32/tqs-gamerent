package gamerent.config;

import gamerent.data.User;
import gamerent.data.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.init.demoPassword:}")
    private String demoPassword;

    @Value("${app.init.adminPassword:}")
    private String adminPassword;

    public DataInitializer(ItemRepository itemRepository, ItemService itemService, UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, JdbcTemplate jdbcTemplate) {
        this.itemRepository = itemRepository;
        this.itemService = itemService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        ensureReviewConstraint();
        User demoUser;
        if (userRepository.count() == 0) {
            // Create two default users: a regular user and an admin
            User demo = new User();
            demo.setName("Demo User");
            demo.setEmail("demo@gamerent.com");
            demo.setPassword(passwordEncoder.encode(demoPassword));
            demo.setRole("USER");
            userRepository.save(demo);

            User admin = new User();
            admin.setName("Admin");
            admin.setEmail("admin@gamerent.com");
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole("ADMIN");
            userRepository.save(admin);

            demoUser = demo;
        } else {
            demoUser = userRepository.findAll().get(0);
        }

        if (itemRepository.count() == 0) {
            // Populate ALL games from IGDB (no limit - fetches everything available)
            itemService.populateFromIGDB(0, demoUser);
        }
    }

    private void ensureReviewConstraint() {
        try {
            jdbcTemplate.execute("ALTER TABLE review DROP CONSTRAINT IF EXISTS review_target_type_check");
            jdbcTemplate.execute("ALTER TABLE review ALTER COLUMN target_type TYPE VARCHAR(20)");
            jdbcTemplate.execute("ALTER TABLE review ADD CONSTRAINT review_target_type_check CHECK (target_type IN ('ITEM','USER'))");
        } catch (Exception e) {
            System.err.println("Failed to adjust review constraint: " + e.getMessage());
        }
    }
}
