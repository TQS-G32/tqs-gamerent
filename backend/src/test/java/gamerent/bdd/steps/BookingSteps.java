package gamerent.bdd.steps;

import gamerent.data.*;
import gamerent.service.BookingService;
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
public class BookingSteps {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private BookingService bookingService;

    private User renter;
    private User owner;
    private Item item;
    private BookingRequest booking;

    private User createOrGetUser(String email) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User user = new User();
            user.setEmail(email);
            user.setName("User");
            user.setPassword("password");
            user.setRole(email.contains("owner") ? "OWNER" : "RENTER");
            return userRepository.save(user);
        });
    }

    @Given("a user {string} exists")
    public void a_user_exists(String email) {
        User u = createOrGetUser(email);
        if (email.contains("owner")) owner = u;
        else renter = u;
    }

    @Given("an item {string} exists owned by {string}")
    public void an_item_exists_owned_by(String itemName, String ownerEmail) {
        owner = createOrGetUser(ownerEmail);
        
        item = new Item();
        item.setName(itemName);
        item.setOwner(owner);
        item.setPricePerDay(10.0);
        item.setCategory("Console");
        item.setAvailable(true);
        itemRepository.save(item);
    }

    @When("the user {string} books {string} for tomorrow")
    public void the_user_books_for_tomorrow(String renterEmail, String itemName) {
        renter = createOrGetUser(renterEmail);
        booking = bookingService.createBooking(item.getId(), renter.getId(), LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));
    }

    @Then("the booking status should be {string}")
    public void the_booking_status_should_be(String status) {
        assertEquals(BookingStatus.valueOf(status), booking.getStatus());
    }
}
