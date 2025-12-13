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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
public class OwnerBookingsSteps {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private BookingService bookingService;

    @Given("an owner {string} with an item {string} exists")
    public void owner_with_item_exists(String ownerEmail, String itemName) {
        User owner = userRepository.findByEmail(ownerEmail).orElseGet(() -> {
            User u = new User(); u.setEmail(ownerEmail); u.setName("Owner"); u.setPassword("pw"); u.setRole("OWNER"); return userRepository.save(u);
        });

        Item item = new Item();
        item.setName(itemName);
        item.setOwner(owner);
        item.setCategory("Console");
        item.setPricePerDay(10.0);
        item.setAvailable(true);
        itemRepository.save(item);
    }

    @Given("a future booking exists for item {string} by user {string}")
    public void future_booking_exists(String itemName, String renterEmail) {
        User renter = userRepository.findByEmail(renterEmail).orElseGet(() -> {
            User u = new User(); u.setEmail(renterEmail); u.setName("Renter"); u.setPassword("pw"); u.setRole("RENTER"); return userRepository.save(u);
        });

        Item item = itemRepository.findByNameContainingIgnoreCase(itemName).stream().findFirst().orElseThrow();

        BookingRequest booking = new BookingRequest();
        booking.setItemId(item.getId());
        booking.setUserId(renter.getId());
        booking.setStartDate(LocalDate.now().plusDays(2));
        booking.setEndDate(LocalDate.now().plusDays(4));
        booking.setStatus(BookingStatus.PENDING);
        bookingRepository.save(booking);
    }

    @When("the owner checks their bookings")
    public void owner_checks_bookings() {
        // no-op; check performed in Then step
    }

    @Then("they should see a booking for item {string}")
    public void should_see_booking(String itemName) {
        Item item = itemRepository.findByNameContainingIgnoreCase(itemName).stream().findFirst().orElseThrow();
        List<BookingRequest> bookings = bookingService.getItemBookings(item.getId());
        assertTrue(bookings.size() > 0 && bookings.stream().anyMatch(b -> b.getItemId().equals(item.getId())));
    }
}
