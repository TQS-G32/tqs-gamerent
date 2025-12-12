package gamerent.bdd.steps;

import gamerent.data.*;
import gamerent.service.ReviewService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
public class OwnerReviewSteps {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private ReviewService reviewService;
    @Autowired
    private ReviewRepository reviewRepository;

    private Item item;
    private BookingRequest booking;

    @When("the user {string} leaves a user review with rating {int} and comment {string} for the booking")
    public void owner_leaves_user_review(String email, Integer rating, String comment) {
        User reviewer = userRepository.findByEmail(email).orElseThrow();
        // Try to find a past approved booking for which the reviewer (owner) has not already submitted a user review
        Optional<BookingRequest> opt = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.APPROVED)
                .filter(b -> b.getEndDate() != null && b.getEndDate().isBefore(LocalDate.now()))
                .filter(b -> {
                    var it = itemRepository.findById(b.getItemId()).orElse(null);
                    return it != null && it.getOwner() != null && it.getOwner().getId().equals(reviewer.getId());
                })
                .filter(b -> {
                    Long renterId = b.getUserId();
                    return reviewRepository.findByBookingIdAndReviewerIdAndTargetTypeAndTargetId(b.getId(), reviewer.getId(), ReviewTargetType.USER, renterId).isEmpty();
                })
                .findFirst();

        BookingRequest foundBooking;
        if (opt.isPresent()) {
            foundBooking = opt.get();
        } else {
            // Create a new approved past booking specifically for this scenario to avoid duplicate reviews
            User renter = userRepository.findByEmail("renter-for-owner-review@example.com").orElseGet(() -> {
                User u = new User(); u.setEmail("renter-for-owner-review@example.com"); u.setName("RenterForOwner"); u.setPassword("pw"); u.setRole("RENTER"); return userRepository.save(u);
            });
            User owner = reviewer;
            Item it = new Item();
            it.setName("OwnerReview-" + System.currentTimeMillis());
            it.setOwner(owner);
            it.setCategory("Console");
            it.setPricePerDay(5.0);
            it.setAvailable(true);
            itemRepository.save(it);

            BookingRequest newBooking = new BookingRequest();
            newBooking.setItemId(it.getId());
            newBooking.setUserId(renter.getId());
            newBooking.setStartDate(LocalDate.now().minusDays(5));
            newBooking.setEndDate(LocalDate.now().minusDays(1));
            newBooking.setStatus(BookingStatus.APPROVED);
            bookingRepository.save(newBooking);
            foundBooking = newBooking;
        }

        Review review = new Review();
        review.setBookingId(foundBooking.getId());
        review.setTargetType(ReviewTargetType.USER);
        review.setRating(rating);
        review.setComment(comment);

        reviewService.addReview(reviewer.getId(), review);
    }

    @Then("the user {string} should have a review with rating {int}")
    public void user_should_have_review(String userEmail, Integer rating) {
        User target = userRepository.findByEmail(userEmail).orElseThrow();
        List<gamerent.boundary.dto.ReviewResponse> responses = reviewService.getReviewsForUser(target.getId());
        assertTrue(responses.stream().anyMatch(r -> r.rating().equals(rating)));
    }
}
