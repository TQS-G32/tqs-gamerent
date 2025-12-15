package gamerent.boundary;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import app.getxray.xray.junit.customjunitxml.annotations.XrayTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import gamerent.data.Item;
import gamerent.data.ItemRepository;
import gamerent.data.User;
import gamerent.data.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Requirement("US1, US4, US10")
class ItemControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User testOwner;

    @BeforeEach
    void setUp() {
        // Clean up test data
        itemRepository.deleteAll();
        userRepository.findByEmail("itemowner@example.com").ifPresent(userRepository::delete);

        // Create test owner
        testOwner = new User();
        testOwner.setName("Item Owner");
        testOwner.setEmail("itemowner@example.com");
        testOwner.setPassword("password");
        testOwner.setRole("OWNER");
        testOwner = userRepository.save(testOwner);
    }

    @Test
    @XrayTest(key = "TGR-33")
    @Tag("integration")
    void getCatalog_ShouldReturnAllItems() throws Exception {
        // Create test items
        Item item1 = new Item();
        item1.setName("PlayStation 5");
        item1.setCategory("Console");
        item1.setPricePerDay(15.0);
        item1.setDescription("Gaming console");
        item1.setAvailable(true);
        item1.setOwner(testOwner);
        itemRepository.save(item1);

        Item item2 = new Item();
        item2.setName("Xbox Series X");
        item2.setCategory("Console");
        item2.setPricePerDay(12.0);
        item2.setDescription("Gaming console");
        item2.setAvailable(true);
        item2.setOwner(testOwner);
        itemRepository.save(item2);

        mockMvc.perform(get("/api/items/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.totalCount").value(2));
    }

    @Test
    @XrayTest(key = "TGR-33")
    @Tag("integration")
    void getCatalog_WithSearchQuery_ShouldFilterResults() throws Exception {
        Item item1 = new Item();
        item1.setName("PlayStation 5");
        item1.setCategory("Console");
        item1.setPricePerDay(15.0);
        item1.setOwner(testOwner);
        itemRepository.save(item1);

        Item item2 = new Item();
        item2.setName("Nintendo Switch");
        item2.setCategory("Console");
        item2.setPricePerDay(8.0);
        item2.setOwner(testOwner);
        itemRepository.save(item2);

        mockMvc.perform(get("/api/items/catalog")
                .param("q", "PlayStation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].name").value("PlayStation 5"));
    }

    @Test
    @XrayTest(key = "TGR-33")
    @Tag("integration")
    void getCatalog_WithCategoryFilter_ShouldFilterResults() throws Exception {
        Item console = new Item();
        console.setName("PlayStation 5");
        console.setCategory("Console");
        console.setPricePerDay(15.0);
        console.setOwner(testOwner);
        itemRepository.save(console);

        Item game = new Item();
        game.setName("FIFA 24");
        game.setCategory("Game");
        game.setPricePerDay(3.0);
        game.setOwner(testOwner);
        itemRepository.save(game);

        mockMvc.perform(get("/api/items/catalog")
                .param("category", "Game"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].category").value("Game"));
    }

    @Test
    @XrayTest(key = "TGR-33")
    @Tag("integration")
    void getCatalog_WithRentableFilter_ShouldOnlyReturnAvailableItems() throws Exception {
        Item available = new Item();
        available.setName("Available Item");
        available.setCategory("Console");
        available.setPricePerDay(15.0);
        available.setAvailable(true);
        available.setOwner(testOwner);
        itemRepository.save(available);

        Item unavailable = new Item();
        unavailable.setName("Unavailable Item");
        unavailable.setCategory("Console");
        unavailable.setPricePerDay(15.0);
        unavailable.setAvailable(false);
        unavailable.setOwner(testOwner);
        itemRepository.save(unavailable);

        mockMvc.perform(get("/api/items/catalog")
                .param("rentable", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Available Item"));
    }

    @Test
    @XrayTest(key = "TGR-33")
    @Tag("integration")
    void getCatalog_WithPagination_ShouldReturnCorrectPage() throws Exception {
        // Create multiple items
        for (int i = 1; i <= 15; i++) {
            Item item = new Item();
            item.setName("Item " + i);
            item.setCategory("Console");
            item.setPricePerDay(10.0);
            item.setOwner(testOwner);
            itemRepository.save(item);
        }

        mockMvc.perform(get("/api/items/catalog")
                .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(10))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalCount").value(15))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get("/api/items/catalog")
                .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(5))
                .andExpect(jsonPath("$.page").value(1));
    }

    @Test
    @XrayTest(key = "TGR-34")
    @Tag("integration")
    void getItemById_ShouldReturnItem() throws Exception {
        Item item = new Item();
        item.setName("Test Item");
        item.setCategory("Console");
        item.setPricePerDay(10.0);
        item.setDescription("Test description");
        item.setOwner(testOwner);
        item = itemRepository.save(item);

        mockMvc.perform(get("/api/items/{id}", item.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Item"))
                .andExpect(jsonPath("$.category").value("Console"));
    }

    @Test
    @XrayTest(key = "TGR-34")
    @Tag("integration")
    void getItemById_WithInvalidId_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/items/{id}", 99999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @XrayTest(key = "TGR-35")
    @Tag("integration") 
    void createItem_ShouldCreateNewItem() throws Exception {
        String itemJson = """
            {
                "name": "New Console",
                "category": "Console",
                "pricePerDay": 20.0,
                "description": "Brand new console",
                "available": true
            }
            """;

        mockMvc.perform(post("/api/items")
                .sessionAttr("userId", testOwner.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(itemJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Console"))
                .andExpect(jsonPath("$.pricePerDay").value(20.0));

        // Verify item was created in database
        assertThat(itemRepository.findAll().stream()
                .anyMatch(i -> i.getName().equals("New Console"))).isTrue();
    }

    @Test
    @XrayTest(key = "TGR-35")
    @Tag("integration")
    void createItem_WithoutSession_ShouldReturn401() throws Exception {
        String itemJson = """
            {
                "name": "New Console",
                "category": "Console",
                "pricePerDay": 20.0
            }
            """;

        mockMvc.perform(post("/api/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(itemJson))
                .andExpect(status().isUnauthorized());
    }
}
