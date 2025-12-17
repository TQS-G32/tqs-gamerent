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
class PlaywrightChatIT {
    @LocalServerPort private int serverPort;
    @Autowired private ChatRepository chatRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ItemRepository itemRepository;

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
    void testChatPageLoads() {
        String baseUrl = "http://localhost:3000";
        
        // Navigate to Chats page (without login, should redirect or show message)
        page.navigate(baseUrl + "/chats");
        page.waitForTimeout(1000);
        
        // Either we see Messages heading or redirected to auth
        assertTrue(
            page.isVisible("h1:has-text('Messages')") || page.url().contains("/auth"),
            "Should show Messages page or redirect to auth"
        );
    }

    @Test
    void testChatPageWithoutLogin() {
        String baseUrl = "http://localhost:3000";
        
        page.navigate(baseUrl + "/chats");
        page.waitForTimeout(1000);
        
        // Without login, might redirect to /auth or show empty state
        boolean redirectedToAuth = page.url().contains("/auth");
        boolean hasMessagesPage = page.isVisible("text=Messages");
        
        assertTrue(redirectedToAuth || hasMessagesPage, "Should handle unauthenticated access");
    }
}