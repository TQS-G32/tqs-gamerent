package gamerent.service;

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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserRepository userRepository;

    private ReviewService reviewService;
    private BookingRequest approvedPastBooking;
    private Item item;
    private User owner;
    private Clock fixedClock;

    @BeforeEach
    void setup() {
        fixedClock = Clock.fixed(LocalDate.of(2025, 1, 10).atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        reviewService = new ReviewService(reviewRepository, bookingRepository, itemRepository, userRepository, fixedClock);

        owner = new User();
        owner.setId(99L);
        owner.setName("Owner");

        item = new Item();
        item.setId(10L);
        item.setOwner(owner);

        approvedPastBooking = new BookingRequest();
        approvedPastBooking.setId(1L);
        approvedPastBooking.setItemId(item.getId());
        approvedPastBooking.setUserId(20L);
        approvedPastBooking.setStartDate(LocalDate.of(2025, 1, 1));
        approvedPastBooking.setEndDate(LocalDate.of(2025, 1, 5));
        approvedPastBooking.setStatus(BookingStatus.APPROVED);
    }

    @Test
    void addReview_ItemAfterRentalEnds_ShouldSave() {
        Review review = new Review();
        review.setBookingId(approvedPastBooking.getId());
        review.setTargetType(ReviewTargetType.ITEM);
        review.setRating(5);

        when(bookingRepository.findById(approvedPastBooking.getId())).thenReturn(Optional.of(approvedPastBooking));
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(reviewRepository.findByBookingIdAndReviewerIdAndTargetTypeAndTargetId(anyLong(), anyLong(), any(), anyLong()))
                .thenReturn(Optional.empty());
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Review saved = reviewService.addReview(approvedPastBooking.getUserId(), review);

        assertNotNull(saved.getReviewerId());
        assertEquals(item.getId(), saved.getTargetId());
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void addReview_BeforeRentalEnds_ShouldFail() {
        BookingRequest futureBooking = new BookingRequest();
        futureBooking.setId(2L);
        futureBooking.setItemId(item.getId());
        futureBooking.setUserId(20L);
        futureBooking.setEndDate(LocalDate.of(2025, 1, 20));
        futureBooking.setStatus(BookingStatus.APPROVED);

        Review review = new Review();
        review.setBookingId(futureBooking.getId());
        review.setTargetType(ReviewTargetType.ITEM);
        review.setRating(4);

        when(bookingRepository.findById(futureBooking.getId())).thenReturn(Optional.of(futureBooking));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            reviewService.addReview(futureBooking.getUserId(), review);
        });
        assertEquals("You can only review after the rental period has ended", ex.getMessage());
    }

    @Test
    void addReview_OwnerReviewsRenter_ShouldSave() {
        Review review = new Review();
        review.setBookingId(approvedPastBooking.getId());
        review.setTargetType(ReviewTargetType.USER);
        review.setRating(5);

        when(bookingRepository.findById(approvedPastBooking.getId())).thenReturn(Optional.of(approvedPastBooking));
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(reviewRepository.findByBookingIdAndReviewerIdAndTargetTypeAndTargetId(anyLong(), anyLong(), any(), anyLong()))
                .thenReturn(Optional.empty());
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Review saved = reviewService.addReview(owner.getId(), review);

        assertEquals(approvedPastBooking.getUserId(), saved.getTargetId());
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void addReview_Duplicate_ShouldFail() {
        Review review = new Review();
        review.setBookingId(approvedPastBooking.getId());
        review.setTargetType(ReviewTargetType.ITEM);
        review.setRating(3);

        when(bookingRepository.findById(approvedPastBooking.getId())).thenReturn(Optional.of(approvedPastBooking));
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(reviewRepository.findByBookingIdAndReviewerIdAndTargetTypeAndTargetId(anyLong(), anyLong(), any(), anyLong()))
                .thenReturn(Optional.of(new Review()));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> reviewService.addReview(approvedPastBooking.getUserId(), review));
        assertEquals("You have already submitted this review", ex.getMessage());
    }

    @Test
    void addReview_UnauthorizedParticipant_ShouldFail() {
        Review review = new Review();
        review.setBookingId(approvedPastBooking.getId());
        review.setTargetType(ReviewTargetType.ITEM);
        review.setRating(4);

        when(bookingRepository.findById(approvedPastBooking.getId())).thenReturn(Optional.of(approvedPastBooking));
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> reviewService.addReview(123L, review));
        assertEquals("Only the renter can review this item", ex.getMessage());
    }

    @Test
    void addReview_MissingRating_ShouldFail() {
        Review review = new Review();
        review.setBookingId(approvedPastBooking.getId());
        review.setTargetType(ReviewTargetType.ITEM);
        // rating null

        RuntimeException ex = assertThrows(RuntimeException.class, () -> reviewService.addReview(approvedPastBooking.getUserId(), review));
        assertEquals("rating must be between 1 and 5", ex.getMessage());
    }

    @Test
    void addReview_BookingNotApproved_ShouldFail() {
        BookingRequest pending = new BookingRequest();
        pending.setId(3L);
        pending.setUserId(20L);
        pending.setItemId(item.getId());
        pending.setStatus(BookingStatus.PENDING);

        Review review = new Review();
        review.setBookingId(pending.getId());
        review.setTargetType(ReviewTargetType.ITEM);
        review.setRating(5);

        when(bookingRepository.findById(pending.getId())).thenReturn(Optional.of(pending));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            reviewService.addReview(pending.getUserId(), review);
        });
        assertEquals("Booking must be approved to review", ex.getMessage());
    }

    @Test
    void addReview_BeforeEndDate_ShouldFail() {
        BookingRequest future = new BookingRequest();
        future.setId(4L);
        future.setUserId(20L);
        future.setItemId(item.getId());
        future.setStatus(BookingStatus.APPROVED);
        future.setEndDate(LocalDate.now(fixedClock).plusDays(2));

        Review review = new Review();
        review.setBookingId(future.getId());
        review.setTargetType(ReviewTargetType.ITEM);
        review.setRating(5);

        when(bookingRepository.findById(future.getId())).thenReturn(Optional.of(future));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> reviewService.addReview(future.getUserId(), review));
        assertEquals("You can only review after the rental period has ended", ex.getMessage());
    }

    @Test
    void addReview_RenterReviewsOwner_ShouldSetTargetId() {
        Review review = new Review();
        review.setBookingId(approvedPastBooking.getId());
        review.setTargetType(ReviewTargetType.USER);
        review.setRating(5);

        when(bookingRepository.findById(approvedPastBooking.getId())).thenReturn(Optional.of(approvedPastBooking));
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(reviewRepository.findByBookingIdAndReviewerIdAndTargetTypeAndTargetId(anyLong(), anyLong(), any(), anyLong()))
                .thenReturn(Optional.empty());
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Review saved = reviewService.addReview(approvedPastBooking.getUserId(), review);

        assertEquals(owner.getId(), saved.getTargetId());
        verify(reviewRepository).save(any(Review.class));
    }
}

