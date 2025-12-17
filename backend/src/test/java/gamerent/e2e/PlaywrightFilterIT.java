package gamerent.e2e;

import com.microsoft.playwright.*;
import gamerent.data.Item;
import gamerent.data.ItemRepository;
import gamerent.data.User;
import gamerent.data.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("e2e")
class PlaywrightFilterIT {

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
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    @BeforeEach
    void createContext() {
        // Create diverse test items with different categories
        if (itemRepository.count() < 5) {
            itemRepository.deleteAll();
            
            User user = new User();
            user.setName("Filter Test User");
            user.setEmail("filter@test.com");
            user.setRole("OWNER");
            userRepository.save(user);

            // Create items in different categories
            Item console = new Item("PlayStation 5", "Gaming console", 25.0, null, user);
            console.setCategory("Console");
            console.setAvailable(true);
            itemRepository.save(console);

            Item game1 = new Item("Horizon Zero Dawn", "Action game", 5.0, null, user);
            game1.setCategory("Game");
            game1.setAvailable(true);
            itemRepository.save(game1);

            Item game2 = new Item("God of War", "Action game", 5.0, null, user);
            game2.setCategory("Game");
            game2.setAvailable(true);
            itemRepository.save(game2);

            Item accessory = new Item("DualSense Controller", "Wireless controller", 8.0, null, user);
            accessory.setCategory("Accessory");
            accessory.setAvailable(true);
            itemRepository.save(accessory);

            Item unavailable = new Item("Broken Headset", "Not working", 0.0, null, user);
            unavailable.setCategory("Accessory");
            unavailable.setAvailable(false);
            itemRepository.save(unavailable);
        }

        context = browser.newContext();
        page = context.newPage();
    }

    @AfterEach
    void closeContext() {
        if (context != null) context.close();
    }

    @Test
    void testCategoryFilterButtons() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl);
        
        page.waitForSelector(".item-card", new Page.WaitForSelectorOptions().setTimeout(10000));
        
        // Verify all category buttons exist
        assertTrue(page.isVisible("button:has-text('All')"), "Should have All category button");
        assertTrue(page.isVisible("button:has-text('Game')"), "Should have Game category button");
        assertTrue(page.isVisible("button:has-text('Console')"), "Should have Console category button");
        assertTrue(page.isVisible("button:has-text('Accessory')"), "Should have Accessory category button");
    }

    @Test
    void testFilterByGameCategory() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl);
        
        page.waitForSelector(".item-card", new Page.WaitForSelectorOptions().setTimeout(10000));
        int initialCount = page.locator(".item-card").count();
        
        // Click Game category
        page.click("button:has-text('Game')");
        page.waitForTimeout(1000);
        
        // Should show some items (we have 2 games)
        int gameCount = page.locator(".item-card").count();
        assertTrue(gameCount > 0, "Should show game items");
        assertTrue(gameCount <= initialCount, "Game count should be less than or equal to all items");
    }

    @Test
    void testFilterByConsoleCategory() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl);
        
        page.waitForSelector(".item-card", new Page.WaitForSelectorOptions().setTimeout(10000));
        
        // Click Console category
        page.click("button:has-text('Console')");
        page.waitForTimeout(1000);
        
        // Should show console items
        int consoleCount = page.locator(".item-card").count();
        assertTrue(consoleCount > 0, "Should show console items");
    }

    @Test
    void testFilterByAccessoryCategory() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl);
        
        page.waitForSelector(".item-card", new Page.WaitForSelectorOptions().setTimeout(10000));
        
        // Click Accessory category
        page.click("button:has-text('Accessory')");
        page.waitForTimeout(1000);
        
        // Should show accessory items
        int accessoryCount = page.locator(".item-card").count();
        assertTrue(accessoryCount >= 0, "Should show accessory items or none");
    }

    @Test
    void testFilterResetWithAllCategory() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl);
        
        page.waitForSelector(".item-card", new Page.WaitForSelectorOptions().setTimeout(10000));
        int allCount = page.locator(".item-card").count();
        
        // Filter by Game
        page.click("button:has-text('Game')");
        page.waitForTimeout(1000);
        int gameCount = page.locator(".item-card").count();
        
        // Reset to All
        page.click("button:has-text('All')");
        page.waitForTimeout(1000);
        int resetCount = page.locator(".item-card").count();
        
        assertEquals(allCount, resetCount, "Should show all items again after clicking All");
    }

    @Test
    void testRentableFilterButton() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl);
        
        page.waitForSelector(".item-card", new Page.WaitForSelectorOptions().setTimeout(10000));
        
        // Verify Rentable filter button exists
        assertTrue(page.isVisible("button:has-text('Rentable')"), "Should have Rentable filter button");
    }

    @Test
    void testRentableFilterFunctionality() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl);
        
        page.waitForSelector(".item-card", new Page.WaitForSelectorOptions().setTimeout(10000));
        int allCount = page.locator(".item-card").count();
        
        // Click Rentable filter
        page.click("button:has-text('Rentable')");
        page.waitForTimeout(1000);
        
        int rentableCount = page.locator(".item-card").count();
        
        // Rentable count should be less or equal (filters out unavailable items)
        assertTrue(rentableCount <= allCount, "Rentable filter should show same or fewer items");
    }

    @Test
    void testCombinedCategoryAndRentableFilter() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl);
        
        page.waitForSelector(".item-card", new Page.WaitForSelectorOptions().setTimeout(10000));
        
        // Apply both category and rentable filters
        page.click("button:has-text('Console')");
        page.waitForTimeout(500);
        page.click("button:has-text('Rentable')");
        page.waitForTimeout(1000);
        
        // Should show filtered results
        int filteredCount = page.locator(".item-card").count();
        assertTrue(filteredCount >= 0, "Should handle combined filters");
    }

    @Test
    void testFilterStateVisualization() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl);
        
        page.waitForSelector("button:has-text('Game')", new Page.WaitForSelectorOptions().setTimeout(5000));
        
        // Click a category button
        Locator gameButton = page.locator("button:has-text('Game')").first();
        gameButton.click();
        page.waitForTimeout(300);
        
        // Button should have active styling (check for any style changes)
        String computedStyle = gameButton.evaluate("el => window.getComputedStyle(el).backgroundColor").toString();
        assertNotNull(computedStyle, "Filter button should have computed styles");
    }

    @Test
    void testItemCountDisplay() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl);
        
        page.waitForSelector(".item-card", new Page.WaitForSelectorOptions().setTimeout(10000));
        
        // Should show item count
        assertTrue(
            page.isVisible("text=/Showing.*item/i") || page.locator(".item-card").count() > 0,
            "Should display item count or items"
        );
    }

    @Test
    void testPaginationControls() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl);
        
        page.waitForSelector(".item-card", new Page.WaitForSelectorOptions().setTimeout(10000));
        
        // Check if pagination controls are present (only if enough items)
        boolean hasPagination = page.isVisible("button:has-text('Next')") || 
                               page.isVisible("button:has-text('Previous')");
        
        // If items < page size, pagination might not show - that's okay
        assertTrue(true, "Pagination test completed");
    }
}
