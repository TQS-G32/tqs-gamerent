package gamerent.boundary;

import gamerent.data.Item;
import gamerent.data.User;
import gamerent.data.UserRepository;
import gamerent.service.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemService itemService;

    @MockBean
    private UserRepository userRepository;

    private Item testItem;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");

        testItem = new Item();
        testItem.setId(1L);
        testItem.setName("PlayStation 5");
        testItem.setCategory("Console");
        testItem.setPricePerDay(10.0);
        testItem.setDescription("High performance console");
        testItem.setOwner(testUser);
    }

    @Test
    void getCatalog_ShouldReturnAllItems() throws Exception {
        given(itemService.searchAllItemsPaginated(null, null, 0, 10))
                .willReturn(List.of(testItem));
        given(itemService.getSearchAllItemsResultCount(null, null))
                .willReturn(1);

        mockMvc.perform(get("/api/items/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value("PlayStation 5"))
                .andExpect(jsonPath("$.totalCount").value(1));
    }

    @Test
    void getCatalog_WithSearch_ShouldReturnFiltered() throws Exception {
        given(itemService.searchAllItemsPaginated("PlayStation", null, 0, 10))
                .willReturn(List.of(testItem));
        given(itemService.getSearchAllItemsResultCount("PlayStation", null))
                .willReturn(1);

        mockMvc.perform(get("/api/items/catalog?q=PlayStation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value("PlayStation 5"));
    }

    @Test
    void getCatalog_WithCategory_ShouldReturnFiltered() throws Exception {
        given(itemService.searchAllItemsPaginated(null, "Console", 0, 10))
                .willReturn(List.of(testItem));
        given(itemService.getSearchAllItemsResultCount(null, "Console"))
                .willReturn(1);

        mockMvc.perform(get("/api/items/catalog?category=Console"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].category").value("Console"));
    }

    @Test
    void getAllItems_ShouldReturnPaginated() throws Exception {
        given(itemService.getAllItemsPaginated(0, 10))
                .willReturn(List.of(testItem));
        given(itemService.getTotalItemCount())
                .willReturn(1);

        mockMvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void getItem_ShouldReturnItem() throws Exception {
        given(itemService.getItem(1L))
                .willReturn(testItem);

        mockMvc.perform(get("/api/items/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("PlayStation 5"))
                .andExpect(jsonPath("$.pricePerDay").value(10.0));
    }

    @Test
    void addItem_ShouldCreateNewItem() throws Exception {
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(itemService.addItem(any(Item.class), any(User.class)))
                .willReturn(testItem);

        mockMvc.perform(post("/api/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"PlayStation 5\", \"category\": \"Console\", \"pricePerDay\": 10.0, \"description\": \"High performance console\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("PlayStation 5"));
    }

    @Test
    void getMyItems_ShouldReturnUserItems() throws Exception {
        given(itemService.getItemsByOwner(1L))
                .willReturn(List.of(testItem));

        mockMvc.perform(get("/api/items/my-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("PlayStation 5"));
    }

    @Test
    void search_ShouldReturnSearchResults() throws Exception {
        given(itemService.searchItemsPaginated("PlayStation", null, 0, 10))
                .willReturn(List.of(testItem));
        given(itemService.getSearchResultCount("PlayStation", null))
                .willReturn(1);

        mockMvc.perform(get("/api/items/search?q=PlayStation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value("PlayStation 5"));
    }

    @Test
    void getCatalog_WithPagination_ShouldReturnPage() throws Exception {
        given(itemService.searchAllItemsPaginated(null, null, 1, 10))
                .willReturn(List.of());
        given(itemService.getSearchAllItemsResultCount(null, null))
                .willReturn(0);

        mockMvc.perform(get("/api/items/catalog?page=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1));
    }
}
