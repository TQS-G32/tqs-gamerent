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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
        given(itemService.searchAllItemsByNameAndCategory(null, null))
                .willReturn(List.of(testItem));

        mockMvc.perform(get("/api/items/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value("PlayStation 5"))
                .andExpect(jsonPath("$.totalCount").value(1));
    }

    @Test
    void getCatalog_WithSearch_ShouldReturnFiltered() throws Exception {
        given(itemService.searchAllItemsByNameAndCategory("PlayStation", null))
                .willReturn(List.of(testItem));

        mockMvc.perform(get("/api/items/catalog?q=PlayStation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value("PlayStation 5"));
    }

    @Test
    void getCatalog_WithCategory_ShouldReturnFiltered() throws Exception {
        given(itemService.searchAllItemsByNameAndCategory(null, "Console"))
                .willReturn(List.of(testItem));

        mockMvc.perform(get("/api/items/catalog?category=Console"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].category").value("Console"));
    }

    @Test
    void getAllItems_ShouldReturnPaginated() throws Exception {
        given(itemService.getAllItems())
                .willReturn(List.of(testItem));

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
        given(itemService.searchItems("PlayStation", null))
                .willReturn(List.of(testItem));

        mockMvc.perform(get("/api/items/search?q=PlayStation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value("PlayStation 5"));
    }

    @Test
    void getCatalog_WithPagination_ShouldReturnPage() throws Exception {
        given(itemService.searchAllItemsByNameAndCategory(null, null))
                .willReturn(List.of());

        mockMvc.perform(get("/api/items/catalog?page=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1));
    }

    // Tests for rentable filter functionality
    @Test
    void getCatalog_WithRentableFilter_ShouldReturnOnlyRentableItems() throws Exception {
        Item rentableItem = new Item();
        rentableItem.setId(1L);
        rentableItem.setName("Rentable Console");
        rentableItem.setAvailable(true);
        rentableItem.setPricePerDay(10.0);

        given(itemService.searchAllItemsByNameAndCategory(null, null))
                .willReturn(List.of(rentableItem));

        mockMvc.perform(get("/api/items/catalog?rentable=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].available").value(true))
                .andExpect(jsonPath("$.items[0].pricePerDay").exists());
    }

    @Test
    void getAllItems_WithRentableFilter_ShouldFilterCorrectly() throws Exception {
        Item rentableItem = new Item();
        rentableItem.setId(1L);
        rentableItem.setAvailable(true);
        rentableItem.setPricePerDay(5.0);

        given(itemService.getAllItems()).willReturn(List.of(rentableItem));

        mockMvc.perform(get("/api/items?rentable=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].available").value(true));
    }

    @Test
    void search_WithRentableFilter_ShouldReturnOnlyRentable() throws Exception {
        Item rentableItem = new Item();
        rentableItem.setId(1L);
        rentableItem.setName("PlayStation 5");
        rentableItem.setAvailable(true);
        rentableItem.setPricePerDay(10.0);

        given(itemService.searchItems("PlayStation", null))
                .willReturn(List.of(rentableItem));

        mockMvc.perform(get("/api/items/search?q=PlayStation&rentable=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].available").value(true));
    }

    // Tests for updateItemSettings endpoint
    @Test
    void updateItemSettings_AsOwner_ShouldUpdateSuccessfully() throws Exception {
        Item updatedItem = new Item();
        updatedItem.setId(1L);
        updatedItem.setAvailable(false);
        updatedItem.setMinRentalDays(7);

        given(itemService.updateItemSettings(1L, 1L, false, 7))
                .willReturn(updatedItem);

        mockMvc.perform(put("/api/items/1/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"available\": false, \"minRentalDays\": 7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Settings updated successfully"))
                .andExpect(jsonPath("$.item.available").value(false))
                .andExpect(jsonPath("$.item.minRentalDays").value(7));
    }

    @Test
    void updateItemSettings_OnlyAvailable_ShouldUpdateAvailability() throws Exception {
        Item updatedItem = new Item();
        updatedItem.setId(1L);
        updatedItem.setAvailable(true);

        given(itemService.updateItemSettings(eq(1L), eq(1L), eq(true), isNull()))
                .willReturn(updatedItem);

        mockMvc.perform(put("/api/items/1/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"available\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.item.available").value(true));
    }

    @Test
    void updateItemSettings_OnlyMinDays_ShouldUpdateMinRentalDays() throws Exception {
        Item updatedItem = new Item();
        updatedItem.setId(1L);
        updatedItem.setMinRentalDays(14);

        given(itemService.updateItemSettings(eq(1L), eq(1L), isNull(), eq(14)))
                .willReturn(updatedItem);

        mockMvc.perform(put("/api/items/1/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"minRentalDays\": 14}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.item.minRentalDays").value(14));
    }

    @Test
    void updateItemSettings_NotOwner_ShouldReturnBadRequest() throws Exception {
        given(itemService.updateItemSettings(anyLong(), anyLong(), any(), any()))
                .willThrow(new RuntimeException("Unauthorized: You are not the owner of this item"));

        mockMvc.perform(put("/api/items/1/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"available\": false}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateItemSettings_WithPendingBookings_ShouldReturnBadRequest() throws Exception {
        given(itemService.updateItemSettings(anyLong(), anyLong(), any(), any()))
                .willThrow(new RuntimeException("Item has active or confirmed bookings and cannot be set to Inactive"));

        mockMvc.perform(put("/api/items/1/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"available\": false}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateItemSettings_InvalidMinDays_ShouldReturnBadRequest() throws Exception {
        given(itemService.updateItemSettings(anyLong(), anyLong(), any(), any()))
                .willThrow(new RuntimeException("Minimum rental days must be between 1 and 30"));

        mockMvc.perform(put("/api/items/1/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"minRentalDays\": 35}"))
                .andExpect(status().isBadRequest());
    }
}
