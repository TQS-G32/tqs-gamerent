package gamerent.e2e.pages;

import com.microsoft.playwright.Page;

public class HomePage {
    private final Page page;

    public HomePage(Page page) {
        this.page = page;
    }

    public void navigate(String baseUrl) {
        page.navigate(baseUrl + "/");
    }

    public void search(String query) {
        // Updated selector for new Vinted-style navbar search
        page.fill(".nav-search input", query);
        page.press(".nav-search input", "Enter");
    }

    public void selectFirstItem() {
        // Updated selector for new item card structure
        page.click(".item-card:first-child");
    }
}
