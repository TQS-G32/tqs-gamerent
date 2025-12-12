package gamerent.e2e;

import com.microsoft.playwright.*;
import gamerent.e2e.pages.HomePage;
import gamerent.data.Item;
import gamerent.data.ItemRepository;
import gamerent.data.User;
import gamerent.data.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PlaywrightSearchIT {

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

    @Disabled("")
    @Test
    void testSearchUIShowsResults() {
        String baseUrl = "http://localhost:" + serverPort;

        HomePage homePage = new HomePage(page);
        homePage.navigate(baseUrl);

        homePage.search("PS5");

        try {
            page.waitForSelector(".item-card", new Page.WaitForSelectorOptions().setTimeout(60000));
        } catch (Exception e) {
            System.out.println("Page content at timeout: " + page.content());
            throw e;
        }

        assertTrue(page.isVisible(".item-card"));
    }
}
