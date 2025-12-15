package gamerent.boundary;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import app.getxray.xray.junit.customjunitxml.annotations.XrayTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import gamerent.data.User;
import gamerent.data.UserRepository;
import gamerent.data.Item;
import gamerent.data.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@Requirement("BASE")
class UserControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Clean up test data
        userRepository.findByEmail("usertest1@example.com").ifPresent(userRepository::delete);
        userRepository.findByEmail("usertest2@example.com").ifPresent(userRepository::delete);
        userRepository.findByEmail("profileuser@example.com").ifPresent(userRepository::delete);
        userRepository.findByEmail("noitems@example.com").ifPresent(userRepository::delete);
    }

    @Test
    @XrayTest(key = "TGR-39")
    @Tag("integration")
    void getAllUsers_ShouldReturnAllUsers() throws Exception {
        // Create test users
        User user1 = new User();
        user1.setName("User 1");
        user1.setEmail("usertest1@example.com");
        user1.setPassword("password");
        user1.setRole("USER");
        userRepository.save(user1);

        User user2 = new User();
        user2.setName("User 2");
        user2.setEmail("usertest2@example.com");
        user2.setPassword("password");
        user2.setRole("OWNER");
        userRepository.save(user2);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    @XrayTest(key = "TGR-40")
    @Tag("integration")
    void addUser_ShouldCreateNewUser() throws Exception {
        String userJson = """
            {
                "name": "New User",
                "email": "newuser@example.com",
                "password": "password123",
                "role": "USER"
            }
            """;

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(userJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New User"))
                .andExpect(jsonPath("$.email").value("newuser@example.com"));

        // Verify user was created
        assertThat(userRepository.findByEmail("newuser@example.com")).isPresent();

        // Cleanup
        userRepository.findByEmail("newuser@example.com").ifPresent(userRepository::delete);
    }

    @Test
    @XrayTest(key = "TGR-41")
    @Tag("integration")
    void getUserProfile_ShouldReturnUserWithItems() throws Exception {
        // Create user
        User user = new User();
        user.setName("Profile User");
        user.setEmail("profileuser@example.com");
        user.setPassword("password");
        user.setRole("OWNER");
        user = userRepository.save(user);

        // Create items for this user
        Item item1 = new Item();
        item1.setName("User's Console");
        item1.setCategory("Console");
        item1.setPricePerDay(15.0);
        item1.setOwner(user);
        itemRepository.save(item1);

        Item item2 = new Item();
        item2.setName("User's Game");
        item2.setCategory("Game");
        item2.setPricePerDay(5.0);
        item2.setOwner(user);
        itemRepository.save(item2);

        mockMvc.perform(get("/api/users/{id}/profile", user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.name").value("Profile User"))
                .andExpect(jsonPath("$.email").value("profileuser@example.com"))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(2));

        // Cleanup
        itemRepository.delete(item1);
        itemRepository.delete(item2);
        userRepository.delete(user);
    }

    @Test
    @XrayTest(key = "TGR-41")
    @Tag("integration")
    void getUserProfile_WithInvalidId_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/users/{id}/profile", 99999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @XrayTest(key = "TGR-41")
    @Tag("integration")
    void getUserProfile_WithNoItems_ShouldReturnEmptyItemsList() throws Exception {
        // Create user without items
        User user = new User();
        user.setName("No Items User");
        user.setEmail("noitems@example.com");
        user.setPassword("password");
        user.setRole("USER");
        user = userRepository.save(user);

        mockMvc.perform(get("/api/users/{id}/profile", user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(0));

        // Cleanup
        userRepository.delete(user);
    }
}
