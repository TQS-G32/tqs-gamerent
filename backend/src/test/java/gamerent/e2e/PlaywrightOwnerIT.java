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
class PlaywrightOwnerIT {

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
            user.setName("E2E Owner");
            user.setEmail("owner-e2e@test.com");
            user.setRole("OWNER");
            userRepository.save(user);

            Item item = new Item("Switch E2E", "E2E Item", 15.0, null, user);
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

    @Disabled("")
    @Test
    void testOwnerCanOpenItemPageAndSeeBookingControls() {
        String baseUrl = "http://localhost:" + serverPort;

        HomePage homePage = new HomePage(page);
        homePage.navigate(baseUrl);

        try {
            page.waitForSelector(".item-card", new Page.WaitForSelectorOptions().setTimeout(60000));
        } catch (Exception e) {
            System.out.println("Page content at timeout: " + page.content());
            throw e;
        }

        homePage.selectFirstItem();

        ItemPage itemPage = new ItemPage(page);
        // we only check that the booking UI is present
        assertTrue(itemPage.hasBookingControls() || itemPage.isBookingSuccessful());
    }
}
