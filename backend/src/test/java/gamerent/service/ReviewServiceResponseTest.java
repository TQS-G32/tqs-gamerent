package gamerent.service;

import gamerent.boundary.dto.ReviewResponse;
import gamerent.data.BookingRepository;
import gamerent.data.BookingRequest;
import gamerent.data.BookingStatus;
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

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceResponseTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private UserRepository userRepository;

    private ReviewService reviewService;
    private BookingRequest booking;
    private Item item;
    private Clock clock;

    @BeforeEach
    void setup() {
        clock = Clock.fixed(LocalDate.of(2025, 1, 10).atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        reviewService = new ReviewService(reviewRepository, bookingRepository, itemRepository, userRepository, clock);

        booking = new BookingRequest();
        booking.setId(1L);
        booking.setItemId(5L);
        booking.setUserId(9L);
        booking.setStartDate(LocalDate.of(2025, 1, 1));
        booking.setEndDate(LocalDate.of(2025, 1, 5));
        booking.setStatus(BookingStatus.APPROVED);

        item = new Item();
        item.setId(5L);
        User owner = new User();
        owner.setId(2L);
        item.setOwner(owner);
    }

    @Test
    void getReviewsForItem_ShouldMapReviewerName() {
        Review review = new Review();
        review.setId(10L);
        review.setBookingId(1L);
        review.setReviewerId(9L);
        review.setTargetType(ReviewTargetType.ITEM);
        review.setTargetId(5L);
        review.setRating(5);
        review.setComment("Great");

        User reviewer = new User();
        reviewer.setId(9L);
        reviewer.setName("Alice");

        when(reviewRepository.findByTargetTypeAndTargetId(ReviewTargetType.ITEM, 5L)).thenReturn(List.of(review));
        when(userRepository.findById(9L)).thenReturn(Optional.of(reviewer));

        List<ReviewResponse> responses = reviewService.getReviewsForItem(5L);

        assertEquals(1, responses.size());
        assertEquals("Alice", responses.get(0).reviewerName());
        assertEquals(5, responses.get(0).rating());
    }

    @Test
    void getReviewsForUser_ShouldReturnEmptyWhenNoReviews() {
        when(reviewRepository.findByTargetTypeAndTargetId(ReviewTargetType.USER, 99L)).thenReturn(List.of());
        List<ReviewResponse> responses = reviewService.getReviewsForUser(99L);
        assertEquals(0, responses.size());
    }
}

