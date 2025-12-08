package gamerent.service;

import gamerent.data.Item;
import gamerent.data.ItemRepository;
import gamerent.data.ReviewRepository;
import gamerent.data.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServicePaginatedTest {

    @Mock
    private ItemRepository itemRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private ItemService itemService;

    private Item item1;

    @BeforeEach
    void setup() {
        item1 = new Item();
        item1.setId(10L);
        item1.setName("Test");
    }

    @Test
    void getItemsByOwnerPaginated_ShouldUseRepositoryPageable() {
        Page<Item> page = new PageImpl<>(List.of(item1), PageRequest.of(1, 2), 5);
        when(itemRepository.findByOwnerId(5L, PageRequest.of(1,2))).thenReturn(page);

        List<Item> items = itemService.getItemsByOwnerPaginated(5L, 1, 2);
        assertEquals(1, items.size());
        assertEquals(10L, items.get(0).getId());
    }
}

