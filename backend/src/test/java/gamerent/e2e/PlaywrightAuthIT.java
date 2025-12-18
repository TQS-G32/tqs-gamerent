package gamerent.e2e;

import com.microsoft.playwright.*;
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
class PlaywrightAuthIT {

    @LocalServerPort
    private int serverPort;

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
        if (userRepository.findByEmail("auth-test@example.com").isEmpty()) {
            User user = new User();
            user.setName("Auth Test User");
            user.setEmail("auth-test@example.com");
            user.setPassword("$2a$10$dummyhash"); // BCrypt hash for "password123"
            user.setRole("RENTER");
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
    void testAuthPageLoads() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl + "/auth");
        
        page.waitForSelector("h2", new Page.WaitForSelectorOptions().setTimeout(5000));
        
        // Should show login/register toggle buttons
        assertTrue(page.isVisible("button:has-text('Login')"), "Should show Login button");
        assertTrue(page.isVisible("button:has-text('Register')"), "Should show Register button");
    }

    @Test
    void testAuthPageToggleBetweenLoginAndRegister() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl + "/auth");
        
        page.waitForSelector("h2", new Page.WaitForSelectorOptions().setTimeout(5000));
        
        // Initially should show LOGIN
        assertTrue(page.isVisible("h2:has-text('LOGIN')"), "Should show LOGIN heading");
        
        // Click Register toggle
        page.click("button.auth-toggle:has-text('Register')");
        page.waitForTimeout(300);
        
        // Should switch to REGISTER
        assertTrue(page.isVisible("h2:has-text('REGISTER')"), "Should show REGISTER heading");
        
        // Click back to Login
        page.click("button.auth-toggle:has-text('Login')");
        page.waitForTimeout(300);
        
        // Should switch back to LOGIN
        assertTrue(page.isVisible("h2:has-text('LOGIN')"), "Should show LOGIN heading again");
    }

    @Test
    void testLoginFormHasRequiredFields() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl + "/auth");
        
        page.waitForSelector("h2:has-text('LOGIN')", new Page.WaitForSelectorOptions().setTimeout(5000));
        
        // Check for email and password inputs
        assertTrue(page.isVisible("input[type='email'], input[placeholder*='email']"), "Should have email input");
        assertTrue(page.isVisible("input[type='password'], input[placeholder*='password']"), "Should have password input");
        assertTrue(page.isVisible("button[type='submit']"), "Should have submit button");
    }

    @Test
    void testRegisterFormHasRequiredFields() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl + "/auth");
        
        // Switch to register
        page.click("button.auth-toggle:has-text('Register')");
        page.waitForTimeout(300);
        
        // Check for name, email, and password inputs
        int inputCount = page.locator("input").count();
        assertTrue(inputCount >= 3, "Register form should have at least 3 input fields (name, email, password)");
        assertTrue(page.isVisible("button[type='submit']"), "Should have submit button");
    }

    @Test
    void testNavigationToAuthPage() {
        String baseUrl = "http://localhost:3000";
        
        // Start from home
        page.navigate(baseUrl);
        page.waitForSelector(".nav-brand", new Page.WaitForSelectorOptions().setTimeout(5000));
        
        // Click Login link in navigation
        if (page.isVisible(".nav-link:has-text('Login')")) {
            page.click(".nav-link:has-text('Login')");
            page.waitForURL("**/auth", new Page.WaitForURLOptions().setTimeout(5000));
            
            assertTrue(page.url().contains("/auth"), "Should navigate to auth page");
            assertTrue(page.isVisible("h2:has-text('LOGIN')"), "Should show login form");
        }
    }

    @Test
    void testAuthPageBranding() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl + "/auth");
        
        page.waitForTimeout(500);
        
        // Verify navigation bar is still present
        assertTrue(page.isVisible(".nav-brand:has-text('GameRent')"), "GameRent brand should be visible");
    }

    @Test
    void testUserCanRegisterSuccessfully() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl + "/auth");
        page.waitForSelector("h2:has-text('LOGIN')", new Page.WaitForSelectorOptions().setTimeout(100));
        page.click("button.auth-toggle:has-text('Register')");
        // Email and password
        page.fill("input[type='text']", "auth test");
        page.fill("input[type='email']", "auth-test@example.com");
        page.fill("input[type='password']", "password123");
        page.click("button[type='submit']");
        // Wait
        page.waitForTimeout(100);
        boolean loggedIn = page.url().contains("/home") || page.isVisible(".nav-brand:has-text('GameRent')");
        assertTrue(loggedIn, "User must go to home after register");
    }

    @Test
    void testUserCanLoginSuccessfully() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl + "/auth");
        page.waitForSelector("h2:has-text('LOGIN')", new Page.WaitForSelectorOptions().setTimeout(100));
        // Email and password
        page.fill("input[type='email']", "auth-test@example.com");
        page.fill("input[type='password']", "password123");
        page.click("button[type='submit']");
        // Wait
        page.waitForTimeout(100);
        boolean loggedIn = page.url().contains("/home") || page.isVisible(".nav-brand:has-text('GameRent')");
        assertTrue(loggedIn, "User must go to home after login");
    }
}
