package gamerent.bdd.steps;

import gamerent.data.*;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
public class ItemAvailabilitySteps {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;

    // Reuse the Given step from OwnerBookingsSteps to avoid duplicate step definitions

    @When("the owner marks item {string} unavailable and sets min rental days to {int}")
    public void owner_marks_unavailable(String itemName, Integer minDays) {
        Item item = itemRepository.findByNameContainingIgnoreCase(itemName).stream().findFirst().orElseThrow();
        item.setAvailable(false);
        item.setMinRentalDays(minDays);
        itemRepository.save(item);
    }

    @Then("the item {string} should be unavailable and have minimum rental days {int}")
    public void item_should_be_unavailable(String itemName, Integer minDays) {
        Item item = itemRepository.findByNameContainingIgnoreCase(itemName).stream().findFirst().orElseThrow();
        assertEquals(false, item.getAvailable());
        assertEquals(minDays, item.getMinRentalDays());
    }
}
