package gamerent.service;

import gamerent.data.Item;
import gamerent.data.ItemRepository;
import gamerent.data.User;
import gamerent.data.BookingRepository;
import gamerent.data.BookingRequest;
import gamerent.data.BookingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private IgdbService igdbService;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private ItemService itemService;

    private Item ps5;
    private Item xbox;
    private Item controller;
    private User owner;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);
        owner.setName("Owner");

        ps5 = new Item();
        ps5.setId(1L);
        ps5.setName("PlayStation 5");
        ps5.setCategory("Console");
        ps5.setPricePerDay(10.0);
        ps5.setOwner(owner);

        xbox = new Item();
        xbox.setId(2L);
        xbox.setName("Xbox Series X");
        xbox.setCategory("Console");
        xbox.setPricePerDay(9.0);
        xbox.setOwner(owner);

        controller = new Item();
        controller.setId(3L);
        controller.setName("PlayStation 5 Controller");
        controller.setCategory("Accessory");
        controller.setPricePerDay(2.0);
        controller.setOwner(owner);
    }

    @Test
    void searchItems_WithQuery_ShouldReturnFuzzyMatch() {
        when(itemRepository.fuzzySearchByName("playstatio")).thenReturn(List.of(ps5));

        List<Item> res = itemService.searchItems("playstatio", null);

        assertFalse(res.isEmpty());
        assertTrue(res.stream().anyMatch(i -> i.getName().equals("PlayStation 5")));
    }

    @Test
    void searchItems_ExactMatch_ShouldReturnItem() {
        when(itemRepository.fuzzySearchByName("PlayStation 5")).thenReturn(List.of(ps5));

        List<Item> res = itemService.searchItems("PlayStation 5", null);

        assertEquals(1, res.size());
        assertEquals("PlayStation 5", res.get(0).getName());
    }

    @Test
    void searchItems_WithCategory_ShouldReturnFiltered() {
        when(itemRepository.findByCategoryIgnoreCase("Console")).thenReturn(List.of(ps5, xbox));

        List<Item> res = itemService.searchItems(null, "Console");

        assertEquals(2, res.size());
        assertTrue(res.stream().allMatch(i -> i.getCategory().equals("Console")));
    }

    @Test
    void searchItems_WithQueryAndCategory_ShouldReturnBoth() {
        when(itemRepository.fuzzySearchByNameAndCategory("PlayStation", "Console")).thenReturn(List.of(ps5));

        List<Item> res = itemService.searchItems("PlayStation", "Console");

        assertEquals(1, res.size());
        assertEquals("PlayStation 5", res.get(0).getName());
    }

    @Test
    void searchAllItems_ShouldReturnAll() {
        when(itemRepository.findAll()).thenReturn(List.of(ps5, xbox, controller));

        List<Item> res = itemService.searchAllItemsByNameAndCategory(null, null);

        assertEquals(3, res.size());
    }

    @Test
    void searchItemsPaginated_ShouldReturnPagedResults() {
        when(itemRepository.fuzzySearchByName("PlayStation")).thenReturn(List.of(ps5));

        List<Item> res = itemService.searchItemsPaginated("PlayStation", null, 0, 10);

        assertEquals(1, res.size());
        assertEquals("PlayStation 5", res.get(0).getName());
    }

    @Test
    void getItemsByOwner_ShouldReturnOwnersItems() {
        when(itemRepository.findByOwnerId(1L)).thenReturn(List.of(ps5, xbox, controller));

        List<Item> res = itemService.getItemsByOwner(1L);

        assertEquals(3, res.size());
        assertTrue(res.stream().allMatch(i -> i.getOwner().getId().equals(1L)));
    }

    @Test
    void addItem_ShouldSetOwner() {
        Item newItem = new Item();
        newItem.setName("New Console");
        
        when(itemRepository.save(any(Item.class))).thenReturn(ps5);

        Item res = itemService.addItem(newItem, owner);

        assertNotNull(res.getOwner());
        assertEquals(owner.getId(), res.getOwner().getId());
    }

    @Test
    void getItem_ShouldReturnItemById() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(ps5));

        Item res = itemService.getItem(1L);

        assertEquals("PlayStation 5", res.getName());
        assertEquals(10.0, res.getPricePerDay());
    }

    @Test
    void getItem_NotFound_ShouldThrowException() {
        when(itemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> itemService.getItem(999L));
    }

    @Test
    void getTotalItemCount_ShouldReturnCount() {
        when(itemRepository.count()).thenReturn(3L);

        int count = itemService.getTotalItemCount();

        assertEquals(3, count);
    }

    @Test
    void searchAllItemsPaginated_ShouldReturnPage() {
        when(itemRepository.findAll()).thenReturn(List.of(ps5, xbox, controller));

        List<Item> page1 = itemService.searchAllItemsPaginated(null, null, 0, 2);

        assertEquals(2, page1.size());
        assertEquals(ps5.getId(), page1.get(0).getId());
        assertEquals(xbox.getId(), page1.get(1).getId());
    }

    @Test
    void searchAllItemsResultCount_ShouldReturnTotal() {
        when(itemRepository.findAll()).thenReturn(List.of(ps5, xbox, controller));

        int count = itemService.getSearchAllItemsResultCount(null, null);

        assertEquals(3, count);
    }

    @Test
    void getAllItemsPaginated_FirstPage_ShouldReturnFirstTwoItems() {
        when(itemRepository.findAll()).thenReturn(List.of(ps5, xbox, controller));

        List<Item> result = itemService.getAllItemsPaginated(0, 2);

        assertEquals(2, result.size());
        assertEquals(ps5.getId(), result.get(0).getId());
        assertEquals(xbox.getId(), result.get(1).getId());
    }

    @Test
    void getAllItemsPaginated_SecondPage_ShouldReturnRemainingItem() {
        when(itemRepository.findAll()).thenReturn(List.of(ps5, xbox, controller));

        List<Item> result = itemService.getAllItemsPaginated(1, 2);

        assertEquals(1, result.size());
        assertEquals(controller.getId(), result.get(0).getId());
    }

    @Test
    void getAllItemsPaginated_PageBeyondItems_ShouldReturnEmptyList() {
        when(itemRepository.findAll()).thenReturn(List.of(ps5, xbox, controller));

        List<Item> result = itemService.getAllItemsPaginated(5, 2);

        assertTrue(result.isEmpty());
    }

    // Tests for updateItemSettings functionality
    @Test
    void updateItemSettings_AsOwner_ShouldUpdateAvailability() {
        ps5.setAvailable(true);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(ps5));
        when(bookingRepository.findByItemId(1L)).thenReturn(List.of());
        when(itemRepository.save(any(Item.class))).thenReturn(ps5);

        Item result = itemService.updateItemSettings(1L, 1L, false, null);

        assertFalse(result.getAvailable());
        verify(itemRepository).save(ps5);
    }

    @Test
    void updateItemSettings_AsOwner_ShouldUpdateMinRentalDays() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(ps5));
        when(itemRepository.save(any(Item.class))).thenAnswer(i -> i.getArguments()[0]);

        Item result = itemService.updateItemSettings(1L, 1L, null, 7);

        assertEquals(7, result.getMinRentalDays());
        verify(itemRepository).save(ps5);
    }

    @Test
    void updateItemSettings_AsOwner_ShouldUpdateBothFields() {
        ps5.setAvailable(false);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(ps5));
        when(itemRepository.save(any(Item.class))).thenAnswer(i -> i.getArguments()[0]);

        Item result = itemService.updateItemSettings(1L, 1L, true, 5);

        assertTrue(result.getAvailable());
        assertEquals(5, result.getMinRentalDays());
        verify(itemRepository).save(ps5);
    }

    @Test
    void updateItemSettings_NotOwner_ShouldThrowException() {
        User otherUser = new User();
        otherUser.setId(2L);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(ps5));

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> itemService.updateItemSettings(1L, 2L, false, null));
        
        assertTrue(exception.getMessage().contains("Unauthorized"));
        verify(itemRepository, never()).save(any());
    }

    @Test
    void updateItemSettings_ItemNotFound_ShouldThrowException() {
        when(itemRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> itemService.updateItemSettings(999L, 1L, false, null));
        
        assertTrue(exception.getMessage().contains("Item not found"));
    }

    @Test
    void updateItemSettings_MinDaysTooLow_ShouldThrowException() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(ps5));

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> itemService.updateItemSettings(1L, 1L, null, 0));
        
        assertTrue(exception.getMessage().contains("Minimum rental days must be between 1 and 30"));
        verify(itemRepository, never()).save(any());
    }

    @Test
    void updateItemSettings_MinDaysTooHigh_ShouldThrowException() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(ps5));

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> itemService.updateItemSettings(1L, 1L, null, 31));
        
        assertTrue(exception.getMessage().contains("Minimum rental days must be between 1 and 30"));
        verify(itemRepository, never()).save(any());
    }

    @Test
    void updateItemSettings_DeactivateWithPendingBooking_ShouldThrowException() {
        BookingRequest pendingBooking = new BookingRequest();
        pendingBooking.setStatus(BookingStatus.PENDING);
        pendingBooking.setEndDate(LocalDate.now().plusDays(5));
        
        when(itemRepository.findById(1L)).thenReturn(Optional.of(ps5));
        when(bookingRepository.findByItemId(1L)).thenReturn(List.of(pendingBooking));

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> itemService.updateItemSettings(1L, 1L, false, null));
        
        assertTrue(exception.getMessage().contains("active or confirmed bookings"));
        verify(itemRepository, never()).save(any());
    }

    @Test
    void updateItemSettings_DeactivateWithApprovedBooking_ShouldThrowException() {
        BookingRequest approvedBooking = new BookingRequest();
        approvedBooking.setStatus(BookingStatus.APPROVED);
        approvedBooking.setEndDate(LocalDate.now().plusDays(3));
        
        when(itemRepository.findById(1L)).thenReturn(Optional.of(ps5));
        when(bookingRepository.findByItemId(1L)).thenReturn(List.of(approvedBooking));

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> itemService.updateItemSettings(1L, 1L, false, null));
        
        assertTrue(exception.getMessage().contains("active or confirmed bookings"));
        verify(itemRepository, never()).save(any());
    }

    @Test
    void updateItemSettings_DeactivateWithPastBooking_ShouldSucceed() {
        BookingRequest pastBooking = new BookingRequest();
        pastBooking.setStatus(BookingStatus.APPROVED);
        pastBooking.setEndDate(LocalDate.now().minusDays(5));
        
        ps5.setAvailable(true);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(ps5));
        when(bookingRepository.findByItemId(1L)).thenReturn(List.of(pastBooking));
        when(itemRepository.save(any(Item.class))).thenReturn(ps5);

        Item result = itemService.updateItemSettings(1L, 1L, false, null);

        assertFalse(result.getAvailable());
        verify(itemRepository).save(ps5);
    }

    @Test
    void updateItemSettings_DeactivateWithRejectedBooking_ShouldSucceed() {
        BookingRequest rejectedBooking = new BookingRequest();
        rejectedBooking.setStatus(BookingStatus.REJECTED);
        rejectedBooking.setEndDate(LocalDate.now().plusDays(5));
        
        ps5.setAvailable(true);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(ps5));
        when(bookingRepository.findByItemId(1L)).thenReturn(List.of(rejectedBooking));
        when(itemRepository.save(any(Item.class))).thenReturn(ps5);

        Item result = itemService.updateItemSettings(1L, 1L, false, null);

        assertFalse(result.getAvailable());
        verify(itemRepository).save(ps5);
    }

    @Test
    void updateItemSettings_ActivateItem_ShouldSucceed() {
        ps5.setAvailable(false);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(ps5));
        when(itemRepository.save(any(Item.class))).thenReturn(ps5);

        Item result = itemService.updateItemSettings(1L, 1L, true, null);

        assertTrue(result.getAvailable());
        verify(itemRepository).save(ps5);
    }

    @Test
    void updateItemSettings_MinDaysAtBoundaryLow_ShouldSucceed() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(ps5));
        when(itemRepository.save(any(Item.class))).thenAnswer(i -> i.getArguments()[0]);

        Item result = itemService.updateItemSettings(1L, 1L, null, 1);

        assertEquals(1, result.getMinRentalDays());
        verify(itemRepository).save(ps5);
    }

    @Test
    void updateItemSettings_MinDaysAtBoundaryHigh_ShouldSucceed() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(ps5));
        when(itemRepository.save(any(Item.class))).thenAnswer(i -> i.getArguments()[0]);

        Item result = itemService.updateItemSettings(1L, 1L, null, 30);

        assertEquals(30, result.getMinRentalDays());
        verify(itemRepository).save(ps5);
    }

    @Test
    void populateFromIGDB_WithGames_ShouldSaveItems() {
        String mockGameJson = """
            [
                {
                    "id": 1,
                    "name": "Test Game",
                    "summary": "A test game",
                    "cover": {
                        "url": "//images.igdb.com/igdb/image/upload/t_thumb/test.jpg"
                    }
                }
            ]
            """;
        
        com.fasterxml.jackson.databind.JsonNode[] games = new com.fasterxml.jackson.databind.JsonNode[1];
        try {
            games[0] = new com.fasterxml.jackson.databind.ObjectMapper().readTree(mockGameJson).get(0);
        } catch (Exception e) {
            // Ignore
        }

        when(igdbService.getPopularGames(0)).thenReturn(java.util.Arrays.asList(games));
        when(itemRepository.saveAll(any())).thenReturn(List.of());

        itemService.populateFromIGDB(0, owner);

        verify(itemRepository, times(1)).saveAll(any());
    }

    @Test
    void populateFromIGDB_ShouldSaveConsolesAndAccessories() {
        when(igdbService.getPopularGames(0)).thenReturn(List.of());
        when(itemRepository.saveAll(any())).thenReturn(List.of());

        itemService.populateFromIGDB(0, owner);

        verify(itemRepository, times(1)).saveAll(any());
    }
}
