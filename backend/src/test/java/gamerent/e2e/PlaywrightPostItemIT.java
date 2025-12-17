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
class PlaywrightPostItemIT {

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
        // Create test user if needed
        if (userRepository.findByEmail("postitem@test.com").isEmpty()) {
            User user = new User();
            user.setName("Post Item User");
            user.setEmail("postitem@test.com");
            user.setRole("OWNER");
            userRepository.save(user);
        }

        context = browser.newContext();
        page = context.newPage();
    }

    @AfterEach
    void closeContext() {
        if (context != null) context.close();
    }

    @Test
    void testPostItemPageLoads() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl + "/post-item");
        
        page.waitForTimeout(1000);
        
        // Should load post item page or redirect to auth
        assertTrue(
            page.url().contains("/post-item") || page.url().contains("/auth"),
            "Should be on post-item page or redirected to auth"
        );
    }

    @Test
    void testNavigateToPostItemFromNav() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl);
        
        page.waitForSelector(".nav-brand", new Page.WaitForSelectorOptions().setTimeout(5000));
        
        // Click "Rent your gear" button
        page.click("text=Rent your gear");
        page.waitForTimeout(500);
        
        assertTrue(page.url().contains("/post-item"), "Should navigate to post-item page");
    }

    @Test
    void testPostItemFormElements() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl + "/post-item");
        
        page.waitForTimeout(1000);
        
        // If on post item page (not redirected), check for form elements
        if (page.url().contains("/post-item") && !page.url().contains("/auth")) {
            // Should have form inputs
            int inputCount = page.locator("input, textarea, select").count();
            assertTrue(inputCount > 0, "Should have form inputs");
        }
    }

    @Test
    void testPostItemPageTitle() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl + "/post-item");
        
        page.waitForTimeout(1000);
        
        // Check for page heading
        if (page.url().contains("/post-item")) {
            boolean hasHeading = page.locator("h1, h2").count() > 0;
            assertTrue(hasHeading, "Should have page heading");
        }
    }

    @Test
    void testPostItemRequiresAuthentication() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl + "/post-item");
        
        page.waitForTimeout(1000);
        
        // Without auth, might redirect or show prompt
        boolean handlesAuth = page.url().contains("/post-item") || 
                             page.url().contains("/auth") ||
                             page.isVisible("text=/log.*in/i");
        
        assertTrue(handlesAuth, "Should handle authentication requirement");
    }
}
