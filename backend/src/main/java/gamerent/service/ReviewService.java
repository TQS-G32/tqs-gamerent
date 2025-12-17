package gamerent.service;

import gamerent.data.BookingRequest;
import gamerent.data.BookingRepository;
import gamerent.data.BookingStatus;
import gamerent.data.Item;
import gamerent.data.ItemRepository;
import gamerent.data.Review;
import gamerent.data.ReviewRepository;
import gamerent.data.ReviewTargetType;
import gamerent.data.User;
import gamerent.data.UserRepository;
import gamerent.boundary.dto.ReviewResponse;
import gamerent.config.ReviewValidationException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public ReviewService(
            ReviewRepository reviewRepository,
            BookingRepository bookingRepository,
            ItemRepository itemRepository,
            UserRepository userRepository,
            Clock clock
    ) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    public Review addReview(Long reviewerId, Review review) {
        validateRequiredFields(review);

        BookingRequest booking = bookingRepository.findById(review.getBookingId())
                .orElseThrow(() -> new NoSuchElementException("Booking not found"));

        if (booking.getStatus() != BookingStatus.APPROVED) {
            throw new ReviewValidationException("Booking must be approved to review");
        }

        LocalDate today = LocalDate.now(clock);
        if (booking.getEndDate() == null || booking.getEndDate().isAfter(today)) {
            throw new ReviewValidationException("You can only review after the rental period has ended");
        }

        Item item = itemRepository.findById(booking.getItemId())
                .orElseThrow(() -> new NoSuchElementException("Item not found"));

        enforceParticipantRules(reviewerId, review, booking, item);

        review.setReviewerId(reviewerId);

        reviewRepository.findByBookingIdAndReviewerIdAndTargetTypeAndTargetId(
                review.getBookingId(),
                reviewerId,
                review.getTargetType(),
                review.getTargetId()
        ).ifPresent(existing -> {
            throw new ReviewValidationException("You have already submitted this review");
        });

        return reviewRepository.save(review);
    }

    public List<Review> getReviewsByBooking(Long bookingId) {
        return reviewRepository.findByBookingId(bookingId);
    }

    public List<ReviewResponse> getReviewsForItem(Long itemId) {
        return reviewRepository.findByTargetTypeAndTargetId(ReviewTargetType.ITEM, itemId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ReviewResponse> getReviewsForUser(Long userId) {
        return reviewRepository.findByTargetTypeAndTargetId(ReviewTargetType.USER, userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void validateRequiredFields(Review review) {
        if (review.getBookingId() == null) {
            throw new ReviewValidationException("bookingId is required");
        }
        if (review.getTargetType() == null) {
            throw new ReviewValidationException("targetType is required");
        }
        if (review.getRating() == null || review.getRating() < 1 || review.getRating() > 5) {
            throw new ReviewValidationException("rating must be between 1 and 5");
        }
    }

    private void enforceParticipantRules(Long reviewerId, Review review, BookingRequest booking, Item item) {
        ReviewTargetType targetType = review.getTargetType();
        if (targetType == ReviewTargetType.ITEM) {
            validateItemReview(reviewerId, review, booking);
            return;
        }
        if (targetType == ReviewTargetType.USER) {
            validateUserReview(reviewerId, review, booking, item);
            return;
        }
        throw new ReviewValidationException("Unsupported review target");
    }

    private void validateItemReview(Long reviewerId, Review review, BookingRequest booking) {
        Long bookingItemId = booking.getItemId();
        review.setTargetId(review.getTargetId() != null ? review.getTargetId() : bookingItemId);
        if (!booking.getUserId().equals(reviewerId)) {
            throw new ReviewValidationException("Only the renter can review this item");
        }
        if (!bookingItemId.equals(review.getTargetId())) {
            throw new ReviewValidationException("Review target item does not match booking item");
        }
    }

    private void validateUserReview(Long reviewerId, Review review, BookingRequest booking, Item item) {
        Long ownerId = item.getOwner() != null ? item.getOwner().getId() : null;
        Long renterId = booking.getUserId();
        boolean reviewerIsRenter = renterId != null && renterId.equals(reviewerId);
        boolean reviewerIsOwner = ownerId != null && ownerId.equals(reviewerId);

        if (!reviewerIsRenter && !reviewerIsOwner) {
            throw new ReviewValidationException("Only booking participants can review each other");
        }

        if (reviewerIsRenter) {
            setTargetOrThrow(review, ownerId, "You can only review the owner for this booking");
        } else {
            setTargetOrThrow(review, renterId, "You can only review the renter for this booking");
        }
    }

    private void setTargetOrThrow(Review review, Long expectedTargetId, String errorMessage) {
        review.setTargetId(review.getTargetId() != null ? review.getTargetId() : expectedTargetId);
        if (review.getTargetId() == null || !review.getTargetId().equals(expectedTargetId)) {
            throw new ReviewValidationException(errorMessage);
        }
    }

    private ReviewResponse toResponse(Review review) {
        String reviewerName = userRepository.findById(review.getReviewerId())
                .map(User::getName)
                .orElse("Member " + review.getReviewerId());
        return new ReviewResponse(
                review.getId(),
                review.getBookingId(),
                review.getReviewerId(),
                reviewerName,
                review.getTargetType(),
                review.getTargetId(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}
