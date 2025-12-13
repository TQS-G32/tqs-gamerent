package gamerent.bdd.steps;

import gamerent.data.Item;
import gamerent.data.ItemRepository;
import gamerent.data.User;
import gamerent.data.UserRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
public class ItemSteps {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;

    @Given("a user {string} exists as owner")
    public void a_user_exists_as_owner(String email) {
        userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User(); u.setEmail(email); u.setName("Owner"); u.setPassword("pw"); u.setRole("OWNER"); return userRepository.save(u);
        });
    }

    @When("the owner registers an item with name {string} category {string} and price {double}")
    public void owner_registers_item(String name, String category, Double price) {
        User owner = userRepository.findByEmail("owner@example.com").orElseThrow();
        Item item = new Item();
        item.setName(name);
        item.setCategory(category);
        item.setPricePerDay(price);
        item.setOwner(owner);
        item.setAvailable(true);
        itemRepository.save(item);
    }

    @Then("the item {string} should exist")
    public void item_should_exist(String name) {
        assertTrue(itemRepository.findByNameContainingIgnoreCase(name).stream().anyMatch(i -> i.getName().equals(name)));
    }
}
