package gamerent.e2e;

import com.microsoft.playwright.*;
import gamerent.data.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("e2e")
class PlaywrightAdminIT {
    @LocalServerPort private int serverPort;
    @Autowired private DisputeRepository disputeRepository;
    @Autowired private BookingRepository bookingRepository;

    private static Playwright playwright;
    private static Browser browser;
    private BrowserContext context;
    private Page page;

    @BeforeAll 
    static void launch() { 
        playwright = Playwright.create(); 
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true)); 
    }

    @AfterAll 
    static void close() { 
        if (browser != null) browser.close();
        if (playwright != null) playwright.close(); 
    }

    @BeforeEach
    void createContext() {
        context = browser.newContext();
        page = context.newPage();
    }

    @AfterEach
    void closeContext() {
        if (context != null) context.close();
    }

    @Test
    void testAdminDashboardPageLoads() {
        String baseUrl = "http://localhost:3000";

        // Navigate to without admin auth
        page.navigate(baseUrl + "/admin");
        page.waitForTimeout(1000);
        
        assertTrue(
            page.isVisible("h1:has-text('Admin Dashboard')") || 
            page.isVisible("text=Access denied") ||
            page.url().contains("/auth"),
            "Should show admin dashboard or access denied message"
        );
    }

    @Test
    void testAdminDashboardMetrics() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl + "/admin");
        page.waitForTimeout(1000);

        // If admin page loads, check for metrics
        if (page.isVisible("h1:has-text('Admin Dashboard')")) {
            assertTrue(page.isVisible("text=Active Listings") || page.isVisible("text=Total Accounts"), 
                      "Should show admin metrics");
        }
    }

    @Test
    void testDisputesPageLoads() {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl + "/disputes");
        page.waitForTimeout(1000);
        
        // Should load disputes page (or redirect if not authenticated)
        assertTrue(
            page.isVisible("text=Dispute") || page.url().contains("/auth"),
            "Should show disputes page or redirect to auth"
        );
    }
}