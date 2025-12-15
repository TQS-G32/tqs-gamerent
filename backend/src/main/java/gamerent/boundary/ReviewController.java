package gamerent.boundary;

import gamerent.data.Review;
import gamerent.service.ReviewService;
import gamerent.boundary.dto.ReviewResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "*")
public class ReviewController {
    private static final String USER_ID_KEY = "userId";

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public Review addReview(@RequestBody Review review, HttpServletRequest request) {
        Long reviewerId = resolveUserId(request);
        try {
            return reviewService.addReview(reviewerId, review);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/booking/{bookingId}")
    public List<Review> getReviews(@PathVariable Long bookingId) {
        return reviewService.getReviewsByBooking(bookingId);
    }

    @GetMapping("/item/{itemId}")
    public List<ReviewResponse> getReviewsForItem(@PathVariable Long itemId) {
        return reviewService.getReviewsForItem(itemId);
    }

    @GetMapping("/user/{userId}")
    public List<ReviewResponse> getReviewsForUser(@PathVariable Long userId) {
        return reviewService.getReviewsForUser(userId);
    }

    private Long resolveUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        Object uid = session.getAttribute(USER_ID_KEY);
        if (uid instanceof Long longValue) return longValue;
        if (uid instanceof Integer intValue) return intValue.longValue();
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
    }
}
