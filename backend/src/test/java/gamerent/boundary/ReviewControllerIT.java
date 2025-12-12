package gamerent.boundary;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import app.getxray.xray.junit.customjunitxml.annotations.XrayTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import gamerent.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@Requirement("US3, US6")
class ReviewControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User owner;
    private User renter;
    private Item item;
    private BookingRequest booking;

    @BeforeEach
    void setUp() {
        // Clean up
        reviewRepository.deleteAll();
        bookingRepository.deleteAll();
        itemRepository.deleteAll();
        userRepository.findByEmail("reviewowner@example.com").ifPresent(userRepository::delete);
        userRepository.findByEmail("reviewrenter@example.com").ifPresent(userRepository::delete);

        // Create owner
        owner = new User();
        owner.setName("Review Owner");
        owner.setEmail("reviewowner@example.com");
        owner.setPassword("password");
        owner.setRole("OWNER");
        owner = userRepository.save(owner);

        // Create renter
        renter = new User();
        renter.setName("Review Renter");
        renter.setEmail("reviewrenter@example.com");
        renter.setPassword("password");
        renter.setRole("RENTER");
        renter = userRepository.save(renter);

        // Create item
        item = new Item();
        item.setName("Review Test Console");
        item.setDescription("Console for review testing");
        item.setCategory("Console");
        item.setPricePerDay(15.0);
        item.setAvailable(true);
        item.setOwner(owner);
        item = itemRepository.save(item);

        // Create booking
        booking = new BookingRequest();
        booking.setItemId(item.getId());
        booking.setUserId(renter.getId());
        booking.setStartDate(LocalDate.of(2024, 1, 1));
        booking.setEndDate(LocalDate.of(2024, 1, 5));
        booking.setStatus(BookingStatus.APPROVED);
        booking = bookingRepository.save(booking);
    }

    @Test
    @XrayTest(key = "REVIEW-1")
    @Tag("integration")
    void addReview_ForItem_ShouldCreateReview() throws Exception {
        String reviewJson = """
            {
                "bookingId": %d,
                "targetType": "ITEM",
                "targetId": %d,
                "rating": 5,
                "comment": "Excellent console!"
            }
            """.formatted(booking.getId(), item.getId());

        mockMvc.perform(post("/api/reviews")
                .sessionAttr("userId", renter.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(reviewJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.comment").value("Excellent console!"))
                .andExpect(jsonPath("$.targetType").value("ITEM"));

        // Verify review was created
        assertThat(reviewRepository.findByBookingId(booking.getId())).isNotEmpty();
    }

    @Test
    @XrayTest(key = "REVIEW-2")
    @Tag("integration")
    void addReview_ForUser_ShouldCreateReview() throws Exception {
        String reviewJson = """
            {
                "bookingId": %d,
                "targetType": "USER",
                "targetId": %d,
                "rating": 4,
                "comment": "Great renter!"
            }
            """.formatted(booking.getId(), renter.getId());

        mockMvc.perform(post("/api/reviews")
                .sessionAttr("userId", owner.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(reviewJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.targetType").value("USER"));
    }

    @Test
    @XrayTest(key = "REVIEW-3")
    @Tag("integration")
    void addReview_WithoutSession_ShouldReturn401() throws Exception {
        String reviewJson = """
            {
                "bookingId": %d,
                "targetType": "ITEM",
                "targetId": %d,
                "rating": 5,
                "comment": "Great!"
            }
            """.formatted(booking.getId(), item.getId());

        mockMvc.perform(post("/api/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(reviewJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @XrayTest(key = "REVIEW-4")
    @Tag("integration")
    void getReviewsByBooking_ShouldReturnReviews() throws Exception {
        // Create a review
        Review review = new Review();
        review.setBookingId(booking.getId());
        review.setReviewerId(renter.getId());
        review.setTargetType(ReviewTargetType.ITEM);
        review.setTargetId(item.getId());
        review.setRating(5);
        review.setComment("Great item!");
        reviewRepository.save(review);

        mockMvc.perform(get("/api/reviews/booking/{bookingId}", booking.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].rating").value(5))
                .andExpect(jsonPath("$[0].comment").value("Great item!"));
    }

    @Test
    @XrayTest(key = "REVIEW-5")
    @Tag("integration")
    void getReviewsForItem_ShouldReturnItemReviews() throws Exception {
        // Create reviews for the item
        Review review1 = new Review();
        review1.setBookingId(booking.getId());
        review1.setReviewerId(renter.getId());
        review1.setTargetType(ReviewTargetType.ITEM);
        review1.setTargetId(item.getId());
        review1.setRating(5);
        review1.setComment("Excellent!");
        reviewRepository.save(review1);

        Review review2 = new Review();
        review2.setBookingId(booking.getId());
        review2.setReviewerId(renter.getId());
        review2.setTargetType(ReviewTargetType.ITEM);
        review2.setTargetId(item.getId());
        review2.setRating(4);
        review2.setComment("Very good!");
        reviewRepository.save(review2);

        mockMvc.perform(get("/api/reviews/item/{itemId}", item.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @XrayTest(key = "REVIEW-6")
    @Tag("integration")
    void getReviewsForUser_ShouldReturnUserReviews() throws Exception {
        // Create review for the user
        Review review = new Review();
        review.setBookingId(booking.getId());
        review.setReviewerId(owner.getId());
        review.setTargetType(ReviewTargetType.USER);
        review.setTargetId(renter.getId());
        review.setRating(5);
        review.setComment("Excellent renter!");
        reviewRepository.save(review);

        mockMvc.perform(get("/api/reviews/user/{userId}", renter.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].rating").value(5))
                .andExpect(jsonPath("$[0].targetType").value("USER"));
    }

    @Test
    @XrayTest(key = "REVIEW-7")
    @Tag("integration")
    void getReviewsForItem_WithNoReviews_ShouldReturnEmptyArray() throws Exception {
        mockMvc.perform(get("/api/reviews/item/{itemId}", item.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @XrayTest(key = "REVIEW-8")
    @Tag("integration")
    void addReview_WithInvalidRating_ShouldReturnBadRequest() throws Exception {
        String reviewJson = """
            {
                "bookingId": %d,
                "targetType": "ITEM",
                "targetId": %d,
                "rating": 6,
                "comment": "Invalid rating"
            }
            """.formatted(booking.getId(), item.getId());

        mockMvc.perform(post("/api/reviews")
                .sessionAttr("userId", renter.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(reviewJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @XrayTest(key = "REVIEW-9")
    @Tag("integration")
    void addReview_ForNonExistentBooking_ShouldReturnBadRequest() throws Exception {
        String reviewJson = """
            {
                "bookingId": 99999,
                "targetType": "ITEM",
                "targetId": %d,
                "rating": 5,
                "comment": "Test"
            }
            """.formatted(item.getId());

        mockMvc.perform(post("/api/reviews")
                .sessionAttr("userId", renter.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(reviewJson))
                .andExpect(status().isBadRequest());
    }
}
