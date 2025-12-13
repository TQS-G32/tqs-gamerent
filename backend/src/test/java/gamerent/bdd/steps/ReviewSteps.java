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

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
public class ReviewSteps {

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
    private User renter;
    private User owner;

    @Given("an approved past booking exists for item {string} with renter {string} and owner {string}")
    public void approved_past_booking_exists(String itemName, String renterEmail, String ownerEmail) {
        renter = userRepository.findByEmail(renterEmail).orElseGet(() -> {
            User u = new User(); u.setEmail(renterEmail); u.setName("Renter"); u.setPassword("pw"); u.setRole("RENTER"); return userRepository.save(u);
        });
        owner = userRepository.findByEmail(ownerEmail).orElseGet(() -> {
            User u = new User(); u.setEmail(ownerEmail); u.setName("Owner"); u.setPassword("pw"); u.setRole("OWNER"); return userRepository.save(u);
        });

        item = new Item();
        item.setName(itemName);
        item.setOwner(owner);
        item.setCategory("Console");
        item.setPricePerDay(10.0);
        item.setAvailable(true);
        itemRepository.save(item);

        booking = new BookingRequest();
        booking.setItemId(item.getId());
        booking.setUserId(renter.getId());
        booking.setStartDate(LocalDate.now().minusDays(5));
        booking.setEndDate(LocalDate.now().minusDays(1));
        booking.setStatus(BookingStatus.APPROVED);
        bookingRepository.save(booking);
    }

    @When("the user {string} leaves a review with rating {int} and comment {string} for the booking")
    public void user_leaves_review(String email, Integer rating, String comment) {
        User reviewer = userRepository.findByEmail(email).orElseThrow();
        Review review = new Review();
        review.setBookingId(booking.getId());
        review.setTargetType(ReviewTargetType.ITEM);
        review.setRating(rating);
        review.setComment(comment);

        reviewService.addReview(reviewer.getId(), review);
    }

    @Then("the item {string} should have a review with rating {int}")
    public void item_should_have_review(String itemName, Integer rating) {
        // ensure review exists for the booking we created
        var reviews = reviewRepository.findByBookingId(booking.getId());
        assertEquals(true, reviews.stream().anyMatch(r -> r.getRating().equals(rating)));
    }
}
