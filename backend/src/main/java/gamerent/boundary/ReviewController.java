package gamerent.boundary;

import gamerent.data.Review;
import gamerent.data.User;
import gamerent.data.UserRepository;
import gamerent.service.ReviewService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "*")
public class ReviewController {
    private final ReviewService reviewService;
    private final UserRepository userRepository;

    public ReviewController(ReviewService reviewService, UserRepository userRepository) {
        this.reviewService = reviewService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public Review addReview(@RequestBody Review review) {
        // Default to user ID 1 (Demo User)
        User reviewer = userRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Default user not found"));
        review.setReviewerId(reviewer.getId());
        return reviewService.addReview(review);
    }

    @GetMapping("/booking/{bookingId}")
    public List<Review> getReviews(@PathVariable Long bookingId) {
        return reviewService.getReviewsByBooking(bookingId);
    }
}
