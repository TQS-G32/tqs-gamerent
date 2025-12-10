package gamerent.service;

import gamerent.boundary.dto.UserProfileResponse;
import gamerent.data.Item;
import gamerent.data.ItemRepository;
import gamerent.data.Review;
import gamerent.data.ReviewRepository;
import gamerent.data.ReviewTargetType;
import gamerent.data.User;
import gamerent.data.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder passwordEncoder;

    private UserService userService;
    private User user;

    @BeforeEach
    void setup() {
        userService = new UserService(userRepository, passwordEncoder, reviewRepository, itemRepository);
        user = new User();
        user.setId(10L);
        user.setName("Bob");
        user.setEmail("bob@mail.com");
    }

    @Test
    void getProfile_ShouldComputeAverageAndCounts() {
        Review r1 = new Review();
        r1.setRating(5);
        r1.setTargetType(ReviewTargetType.USER);
        r1.setTargetId(10L);
        Review r2 = new Review();
        r2.setRating(4);
        r2.setTargetType(ReviewTargetType.USER);
        r2.setTargetId(10L);

        Item item = new Item();
        item.setId(1L);

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(reviewRepository.findByTargetTypeAndTargetId(ReviewTargetType.USER, 10L))
                .thenReturn(List.of(r1, r2));
        when(itemRepository.findByOwnerId(10L)).thenReturn(List.of(item));

        UserProfileResponse profile = userService.getProfile(10L);

        assertEquals(10L, profile.id());
        assertEquals("Bob", profile.name());
        assertEquals(4.5, profile.averageRating());
        assertEquals(2, profile.reviewCount());
        assertEquals(1, profile.itemsCount());
    }
}
