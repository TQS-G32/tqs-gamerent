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
class PlaywrightNavigationIT {

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
        // Ensure test data exists
        if (itemRepository.count() == 0) {
            User user = new User();
            user.setName("Nav Test User");
            user.setEmail("nav@test.com");
            user.setRole("OWNER");
            userRepository.save(user);

            Item item = new Item("Navigation Test Item", "Test Description", 15.0, null, user);
            item.setCategory("Game");
            itemRepository.save(item);
        }

        context = browser.newContext();
        page = context.newPage();
    }

    @AfterEach
    void closeContext() {
        if (context != null) context.close();
    }

    @Test
    void testNavigationBarPresent() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl);
        
        page.waitForSelector(".nav-brand", new Page.WaitForSelectorOptions().setTimeout(5000));
        
        assertTrue(page.isVisible(".nav-brand:has-text('GameRent')"), "GameRent brand should be visible");
        assertTrue(page.isVisible(".nav-search"), "Search bar should be present");
        assertTrue(page.isVisible(".nav-actions"), "Navigation actions should be present");
    }

    @Test
    void testNavigationLinks() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl);
        
        page.waitForSelector(".nav-brand", new Page.WaitForSelectorOptions().setTimeout(5000));
        
        // Check for main navigation links
        assertTrue(page.isVisible("text=My Items"), "My Items link should be visible");
        assertTrue(page.isVisible("text=Disputes"), "Disputes link should be visible");
        assertTrue(page.isVisible("text=Rent your gear"), "Post item link should be visible");
    }

    @Test
    void testClickLogoNavigatesToHome() {
        String baseUrl = "http://localhost:3000";
        
        // Start from a different page
        page.navigate(baseUrl + "/bookings");
        page.waitForTimeout(500);
        
        // Click logo
        page.click(".nav-brand");
        page.waitForTimeout(500);
        
        // Should be back at home
        assertEquals(baseUrl + "/", page.url(), "Should navigate to home page");
    }

    @Test
    void testNavigateToPostItem() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl);
        
        page.waitForSelector(".nav-brand", new Page.WaitForSelectorOptions().setTimeout(5000));
        
        // Click "Rent your gear" button
        page.click("text=Rent your gear");
        page.waitForTimeout(500);
        
        assertTrue(page.url().contains("/post-item"), "Should navigate to post-item page");
    }

    @Test
    void testNavigateToBookings() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl);
        
        page.waitForSelector(".nav-brand", new Page.WaitForSelectorOptions().setTimeout(5000));
        
        // Click "My Items" link
        page.click("text=My Items");
        page.waitForTimeout(500);
        
        assertTrue(page.url().contains("/bookings"), "Should navigate to bookings page");
    }

    @Test
    void testNavigateToDisputes() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl);
        
        page.waitForSelector(".nav-brand", new Page.WaitForSelectorOptions().setTimeout(5000));
        
        // Click "Disputes" link
        page.click(".nav-link:has-text('Disputes')");
        page.waitForTimeout(500);
        
        assertTrue(page.url().contains("/disputes"), "Should navigate to disputes page");
    }

    @Test
    void testSearchBarFunctionality() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl);
        
        page.waitForSelector(".nav-search input", new Page.WaitForSelectorOptions().setTimeout(5000));
        
        // Type in search and press enter
        page.fill(".nav-search input[type='text']", "Test Search");
        page.press(".nav-search input[type='text']", "Enter");
        page.waitForTimeout(500);
        
        // URL should contain search query
        assertTrue(page.url().contains("q=Test"), "URL should contain search query parameter");
    }

    @Test
    void testBreadcrumbNavigationFromItemDetails() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl);
        
        // Wait for items and click first one
        page.waitForSelector(".item-card", new Page.WaitForSelectorOptions().setTimeout(10000));
        page.click(".item-card:first-child");
        page.waitForURL("**/item/**", new Page.WaitForURLOptions().setTimeout(5000));
        
        // Verify we're on item page
        assertTrue(page.url().contains("/item/"), "Should be on item details page");
        
        // Navigate back using browser navigation or logo
        page.click(".nav-brand");
        page.waitForTimeout(500);
        
        // Should be back at home
        assertEquals(baseUrl + "/", page.url(), "Should navigate back to home");
    }

    @Test
    void testResponsiveNavigationElements() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl);
        
        page.waitForSelector(".nav-brand", new Page.WaitForSelectorOptions().setTimeout(5000));
        
        // Verify all main navigation elements are rendered
        assertTrue(page.isVisible(".nav-container"), "Navigation container should be visible");
        assertTrue(page.isVisible(".nav-brand"), "Brand should be visible");
        assertTrue(page.isVisible(".nav-search"), "Search should be visible");
        assertTrue(page.isVisible(".nav-actions"), "Actions should be visible");
    }
}
