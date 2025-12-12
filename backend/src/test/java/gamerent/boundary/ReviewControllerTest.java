package gamerent.boundary;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import app.getxray.xray.junit.customjunitxml.annotations.XrayTest;
import gamerent.data.Review;
import gamerent.data.ReviewTargetType;
import gamerent.service.ReviewService;
import gamerent.boundary.dto.ReviewResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
@Requirement("US3, US6")
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewService reviewService;

    @Test
    @XrayTest(key = "REVIEW-UNIT-1")
    @Tag("unit")
    void addReview_ShouldUseSessionUser() throws Exception {
        Review saved = new Review();
        saved.setId(10L);
        saved.setRating(5);
        saved.setTargetType(ReviewTargetType.ITEM);

        when(reviewService.addReview(eq(5L), any(Review.class))).thenReturn(saved);

        mockMvc.perform(post("/api/reviews")
                .sessionAttr("userId", 5L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bookingId\":1,\"targetType\":\"ITEM\",\"targetId\":2,\"rating\":5,\"comment\":\"Great\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @XrayTest(key = "REVIEW-UNIT-2")
    @Tag("unit")
    void addReview_NoSession_ShouldReturn401() throws Exception {
        mockMvc.perform(post("/api/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bookingId\":1,\"targetType\":\"ITEM\",\"targetId\":2,\"rating\":5}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @XrayTest(key = "REVIEW-UNIT-3")
    @Tag("unit")
    void getItemReviews_ShouldReturnList() throws Exception {
        ReviewResponse review = new ReviewResponse(
                1L,
                10L,
                5L,
                "Alice",
                ReviewTargetType.ITEM,
                2L,
                4,
                "Nice",
                null
        );

        when(reviewService.getReviewsForItem(2L)).thenReturn(List.of(review));

        mockMvc.perform(get("/api/reviews/item/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rating").value(4));
    }
}

