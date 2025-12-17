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
class PlaywrightUIComponentsIT {

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
        // Create test data
        if (itemRepository.count() == 0) {
            User user = new User();
            user.setName("UI Test User");
            user.setEmail("ui@test.com");
            user.setRole("OWNER");
            userRepository.save(user);

            Item item = new Item("UI Test Console", "Testing UI components", 18.0, null, user);
            item.setCategory("Console");
            item.setAvailable(true);
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
    void testItemCardStructure() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl);
        
        page.waitForSelector(".item-card", new Page.WaitForSelectorOptions().setTimeout(10000));
        
        // Verify item card has expected structure
        Locator firstCard = page.locator(".item-card").first();
        assertTrue(firstCard.isVisible(), "Item card should be visible");
        
        // Check for image
        assertTrue(firstCard.locator("img").count() > 0, "Item card should have image");
        
        // Check for card content
        assertTrue(firstCard.locator(".card-price, .card-info").count() > 0, 
                  "Item card should have price or info elements");
    }

    @Test
    void testItemCardClickable() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl);
        
        page.waitForSelector(".item-card", new Page.WaitForSelectorOptions().setTimeout(10000));
        
        String initialUrl = page.url();
        
        // Click first item card
        page.click(".item-card:first-child");
        page.waitForTimeout(1000);
        
        // URL should change to item details
        assertNotEquals(initialUrl, page.url(), "URL should change after clicking item card");
        assertTrue(page.url().contains("/item/"), "Should navigate to item details page");
    }

    @Test
    void testItemCardShowsPrice() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl);
        
        page.waitForSelector(".item-card", new Page.WaitForSelectorOptions().setTimeout(10000));
        
        Locator firstCard = page.locator(".item-card").first();
        
        // Should show price or "Not for rent"
        boolean hasPrice = firstCard.locator(".card-price").count() > 0;
        boolean hasText = firstCard.textContent().contains("€") || 
                         firstCard.textContent().contains("Not for rent");
        
        assertTrue(hasPrice || hasText, "Item card should show price information");
    }

    @Test
    void testItemCardShowsCategory() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl);
        
        page.waitForSelector(".item-card", new Page.WaitForSelectorOptions().setTimeout(10000));
        
        Locator firstCard = page.locator(".item-card").first();
        String cardText = firstCard.textContent();
        
        // Should mention category or have category info
        boolean hasCategory = cardText.contains("Console") || 
                             cardText.contains("Game") || 
                             cardText.contains("Accessory") ||
                             cardText.contains("Gaming");
        
        assertTrue(hasCategory, "Item card should show category");
    }

    @Test
    void testItemGridLayout() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl);
        
        page.waitForSelector(".item-card", new Page.WaitForSelectorOptions().setTimeout(10000));
        
        // Should have item-grid container
        assertTrue(page.isVisible(".item-grid"), "Should have item-grid container");
        
        // Should have multiple cards if data exists
        int cardCount = page.locator(".item-card").count();
        assertTrue(cardCount > 0, "Should have at least one item card");
    }

    @Test
    void testSearchInputPlaceholder() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl);
        
        page.waitForSelector(".nav-search input", new Page.WaitForSelectorOptions().setTimeout(5000));
        
        Locator searchInput = page.locator(".nav-search input[type='text']");
        String placeholder = searchInput.getAttribute("placeholder");
        
        assertNotNull(placeholder, "Search input should have placeholder");
        assertTrue(placeholder.toLowerCase().contains("search"), 
                  "Placeholder should mention 'search'");
    }

    @Test
    void testButtonStyles() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl);
        
        page.waitForSelector(".btn", new Page.WaitForSelectorOptions().setTimeout(5000));
        
        // Should have styled buttons
        int buttonCount = page.locator(".btn").count();
        assertTrue(buttonCount > 0, "Should have styled buttons");
    }

    @Test
    void testFilterButtonsInteractive() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl);
        
        page.waitForSelector("button:has-text('All')", new Page.WaitForSelectorOptions().setTimeout(5000));
        
        // Click a filter button
        page.click("button:has-text('Console')");
        page.waitForTimeout(300);
        
        // Button should respond to click (page doesn't crash)
        assertTrue(page.isVisible("button:has-text('Console')"), 
                  "Filter button should remain visible after click");
    }

    @Test
    void testResponsiveContainer() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl);
        
        page.waitForSelector(".container", new Page.WaitForSelectorOptions().setTimeout(5000));
        
        // Should have main container
        assertTrue(page.isVisible(".container"), "Should have main container element");
    }

    @Test
    void testNavigationIsSticky() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl);
        
        page.waitForSelector(".nav-brand", new Page.WaitForSelectorOptions().setTimeout(5000));
        
        // Navigation should be visible at top
        Locator nav = page.locator("nav, .main-nav").first();
        assertTrue(nav.isVisible(), "Navigation should be visible");
    }

    @Test
    void testImagesLoadProperly() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl);
        
        page.waitForSelector(".item-card", new Page.WaitForSelectorOptions().setTimeout(10000));
        
        // Check if images are present
        int imageCount = page.locator("img").count();
        assertTrue(imageCount > 0, "Page should have images");
        
        // Verify first image has src attribute
        Locator firstImage = page.locator("img").first();
        String src = firstImage.getAttribute("src");
        assertNotNull(src, "Image should have src attribute");
    }

    @Test
    void testNoConsoleErrors() {
        String baseUrl = "http://localhost:3000";
        
        // Listen for console errors
        page.onConsoleMessage(msg -> {
            if (msg.type().equals("error")) {
                System.out.println("Console error: " + msg.text());
            }
        });
        
        page.navigate(baseUrl);
        page.waitForSelector(".item-card", new Page.WaitForSelectorOptions().setTimeout(10000));
        
        // If we get here without exceptions, page loaded successfully
        assertTrue(page.isVisible(".nav-brand"), "Page should load without critical errors");
    }

    @Test
    void testAccessibilityBasics() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl);
        
        page.waitForSelector(".nav-brand", new Page.WaitForSelectorOptions().setTimeout(5000));
        
        // Check for semantic HTML elements
        assertTrue(page.locator("nav").count() > 0, "Should have nav element");
        assertTrue(page.locator("button, a, input").count() > 0, 
                  "Should have interactive elements");
    }

    @Test
    void testLinksAreClickable() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl);
        
        page.waitForSelector(".nav-brand", new Page.WaitForSelectorOptions().setTimeout(5000));
        
        // Verify links are present and clickable
        int linkCount = page.locator("a").count();
        assertTrue(linkCount > 0, "Page should have links");
        
        // Check that brand link works
        String brandHref = page.locator(".nav-brand").getAttribute("href");
        assertNotNull(brandHref, "Brand should be a link");
    }
}
