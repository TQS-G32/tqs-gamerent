package gamerent.bdd.steps;

import gamerent.data.Item;
import gamerent.data.ItemRepository;
import gamerent.service.ItemService;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@Transactional
public class SearchSteps {

    @Autowired
    private ItemRepository itemRepository;
    
    @Autowired
    private ItemService itemService;
    
    private List<Item> searchResults;

    @Given("the following items exist:")
    public void the_following_items_exist(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
        for (Map<String, String> row : rows) {
            Item item = new Item();
            item.setName(row.get("name"));
            item.setCategory(row.get("category"));
            item.setPricePerDay(Double.parseDouble(row.get("price")));
            itemRepository.save(item);
        }
    }

    @When("I search for {string}")
    public void i_search_for(String query) {
        searchResults = itemService.searchItems(query, null);
    }

    @Then("I should find {string}")
    public void i_should_find(String itemName) {
        boolean found = searchResults.stream().anyMatch(i -> i.getName().equals(itemName));
        assertTrue(found, "Expected to find " + itemName);
    }

    @Then("I should not find {string}")
    public void i_should_not_find(String itemName) {
        boolean found = searchResults.stream().anyMatch(i -> i.getName().equals(itemName));
        assertFalse(found, "Expected not to find " + itemName);
    }
}


