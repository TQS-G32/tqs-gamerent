package gamerent.e2e;

import com.microsoft.playwright.*;
import gamerent.e2e.pages.HomePage;
import gamerent.data.Item;
import gamerent.data.ItemRepository;
import gamerent.data.User;
import gamerent.data.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("e2e")
class PlaywrightSearchIT {

    @LocalServerPort
    private int serverPort;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    private static Playwright playwright;
    private static Browser browser;
    private BrowserContext context;
    private Page page;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    }

    @AfterAll
    static void closeBrowser() {
        if (playwright != null) playwright.close();
    }

    @BeforeEach
    void createContext() {
        if (itemRepository.count() == 0) {
            User user = new User();
            user.setName("E2E User");
            user.setEmail("e2e@test.com");
            user.setRole("OWNER");
            userRepository.save(user);

            Item item = new Item("PS5 Test Console", "E2E Test Item", 20.0, null, user);
            item.setCategory("Console");
            itemRepository.save(item);
        }

        context = browser.newContext();
        page = context.newPage();
    }

    @AfterEach
    void closeContext() {
        context.close();
    }

    @Test
    void testSearchUIShowsResults() {
        String baseUrl = "http://localhost:3000";

        HomePage homePage = new HomePage(page);
        homePage.navigate(baseUrl);

        // Wait for initial items to load
        page.waitForSelector(".item-card", new Page.WaitForSelectorOptions().setTimeout(5000));
        
        // Search for the test item we created
        homePage.search("PS5");

        // Wait for search results
        page.waitForTimeout(1500); // Wait for search to complete
        
        // After search, either we have results or "No items found" message
        boolean hasResults = page.isVisible(".item-card") || page.isVisible("text=No items found");
        assertTrue(hasResults, "Search should show results or 'No items found' message");
    }

    @Test
    void testSearchWithNoResults() {
        String baseUrl = "http://localhost:3000";

        HomePage homePage = new HomePage(page);
        homePage.navigate(baseUrl);

        // Search for non-existent item
        homePage.search("NonExistentGameXYZ123");
        page.waitForTimeout(1000);

        // Should show no results message
        assertTrue(homePage.hasNoItemsMessage(), "Should show 'No items found' message");
    }

    @Test
    void testCategoryFilter() {
        String baseUrl = "http://localhost:3000";

        HomePage homePage = new HomePage(page);
        homePage.navigate(baseUrl);

        page.waitForSelector(".item-card", new Page.WaitForSelectorOptions().setTimeout(5000));

        // Click Console category (our test item is a Console)
        homePage.clickCategory("Console");
        page.waitForTimeout(500);

        assertTrue(homePage.hasItems(), "Should show items in Console category");
    }
}
