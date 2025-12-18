package gamerent.e2e;

import com.microsoft.playwright.*;
import gamerent.data.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("e2e")
class PlaywrightChatIT {

    @LocalServerPort
    private int serverPort;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static Playwright playwright;
    private static Browser browser;
    private BrowserContext context;
    private Page page;

    // Test users/items for sharing between tests
    private User owner;
    private User renter;
    private Item item;

    @BeforeAll
    static void launch() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
    }

    @AfterAll
    static void close() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    @BeforeEach
    void setup() {
        // Clear existing data to ensure clean state (Order matters for foreign keys)
        messageRepository.deleteAll();
        chatRepository.deleteAll();
        itemRepository.deleteAll();
        userRepository.deleteAll();

        // Create Owner
        User ownerInput = new User();
        ownerInput.setName("Demo Owner");
        ownerInput.setEmail("demo@gamerent.com");
        ownerInput.setPassword(passwordEncoder.encode("password"));
        ownerInput.setRole("OWNER");
        this.owner = userRepository.save(ownerInput); // CAPTURE SAVED ENTITY WITH ID

        // Create Renter
        User renterInput = new User();
        renterInput.setName("Admin Renter");
        renterInput.setEmail("admin@gamerent.com");
        renterInput.setPassword(passwordEncoder.encode("adminpass"));
        renterInput.setRole("ADMIN");
        this.renter = userRepository.save(renterInput); // CAPTURE SAVED ENTITY WITH ID

        // Create Item owned by Owner
        Item itemInput = new Item();
        itemInput.setName("Chat Test Item");
        itemInput.setDescription("Item specifically for chat testing");
        itemInput.setPricePerDay(15.0);
        itemInput.setAvailable(true);
        itemInput.setOwner(this.owner);
        itemInput.setCategory("Test");
        this.item = itemRepository.save(itemInput); // CAPTURE SAVED ENTITY WITH ID

        context = browser.newContext();
        page = context.newPage();
    }

    @AfterEach
    void closeContext() {
        if (context != null) context.close();
    }

    private void login(String email, String password) {
        String baseUrl = "http://localhost:3000";
        page.navigate(baseUrl + "/auth");
        // Using placeholder selectors as 'name' attributes are missing in React component
        page.fill("input[placeholder='Email']", email);
        page.fill("input[placeholder='Password']", password);
        page.click("button[type='submit']");
        page.waitForURL(url -> !url.contains("/auth"));
    }

    @Test
    void testChatFlow() {
        String baseUrl = "http://localhost:3000";

        // Renter logs in
        login("admin@gamerent.com", "adminpass");

        // Renter starts chat
        page.navigate(baseUrl);
        page.waitForSelector(".item-card");
        page.locator(".item-card").first().click();
        
        page.waitForSelector(".rental-chat-button");
        page.click(".rental-chat-button");
        page.waitForURL(url -> url.contains("/chats/"));

        // Renter sends a message
        String renterMessage = "Hello Owner, is this item available?";
        page.fill("input.message-input", renterMessage);
        page.click("button.send-button");
        page.waitForSelector("text=" + renterMessage);

        // Owner logs in (new context)
        BrowserContext ownerContext = browser.newContext();
        Page ownerPage = ownerContext.newPage();

        // Manual Owner login
        ownerPage.navigate(baseUrl + "/auth");
        ownerPage.fill("input[placeholder='Email']", "demo@gamerent.com");
        ownerPage.fill("input[placeholder='Password']", "password");
        ownerPage.click("button[type='submit']");
        ownerPage.waitForURL(url -> !url.contains("/auth"));

        // Owner checks chat
        ownerPage.navigate(baseUrl + "/chats");
        ownerPage.waitForSelector(".chat-item");
        ownerPage.locator(".chat-item").first().click();

        // Verify Owner sees Renter's message
        ownerPage.waitForSelector("text=" + renterMessage);
        assertTrue(ownerPage.isVisible("text=" + renterMessage), "Owner should see renter's message");

        // Owner replies
        String ownerReply = "Yes, it is ready for rent!";
        ownerPage.fill("input.message-input", ownerReply);
        ownerPage.click("button.send-button");
        ownerPage.waitForSelector("text=" + ownerReply);

        // Verify Renter sees the reply
        page.bringToFront();
        page.reload();
        page.waitForSelector("text=" + ownerReply);
        assertTrue(page.isVisible("text=" + ownerReply), "Renter should see owner's reply");

        ownerContext.close();
    }

    @Test
    void testCannotSendEmptyMessage() {
        String baseUrl = "http://localhost:3000";
        login("admin@gamerent.com", "adminpass");

        // Start chat
        page.navigate(baseUrl);
        page.waitForSelector(".item-card");
        page.locator(".item-card").first().click();
        
        page.waitForSelector(".rental-chat-button");
        page.click(".rental-chat-button");
        
        // Locate input and button
        Locator input = page.locator("input.message-input");
        Locator sendButton = page.locator("button.send-button");

        // 1. Assert button is disabled initially (empty input)
        assertTrue(sendButton.isDisabled(), "Send button should be disabled when input is empty");

        // 2. Type only spaces
        input.fill("   ");
        assertTrue(sendButton.isDisabled(), "Send button should be disabled for whitespace only");

        // 3. Type text
        input.fill("Real message");
        assertFalse(sendButton.isDisabled(), "Send button should be enabled for valid text");
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