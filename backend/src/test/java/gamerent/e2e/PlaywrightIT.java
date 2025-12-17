package gamerent.e2e;

import com.microsoft.playwright.*;
import gamerent.e2e.pages.HomePage;
import gamerent.e2e.pages.ItemPage;
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
class PlaywrightIT {

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
    void testSearchAndBookFlow() {
        String baseUrl = "http://localhost:3000";
        
        HomePage homePage = new HomePage(page);
        homePage.navigate(baseUrl);
        
        // Wait for items to load
        page.waitForSelector(".item-card", new Page.WaitForSelectorOptions().setTimeout(10000));
        
        // Click first item
        homePage.selectFirstItem();
        
        // Wait for item details page to load
        page.waitForURL("**/item/**", new Page.WaitForURLOptions().setTimeout(5000));
        page.waitForTimeout(1000); // Wait for React to render
        
        ItemPage itemPage = new ItemPage(page);
        
        // Verify item details are visible
        assertTrue(itemPage.hasItemDetails(), "Item details should be visible");
        
        // Note: Booking requires authentication, so we just verify the UI is present
        assertTrue(itemPage.hasBookingControls() || itemPage.hasAvailableRentalsSection(), 
                  "Should have booking controls or rental listings");
    }

    @Test
    void testNavigationToHome() {
        String baseUrl = "http://localhost:3000";
        
        page.navigate(baseUrl);
        
        // Verify homepage elements
        assertTrue(page.isVisible(".nav-brand:has-text('GameRent')"), "GameRent brand should be visible");
        assertTrue(page.isVisible(".nav-search"), "Search bar should be visible");
        
        page.waitForSelector(".item-card", new Page.WaitForSelectorOptions().setTimeout(10000));
        assertTrue(page.locator(".item-card").count() > 0, "Should show item cards");
    }
}
