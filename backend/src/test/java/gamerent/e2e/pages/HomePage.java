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
        page.fill(".nav-search input[type='text']", query);
        page.press(".nav-search input[type='text']", "Enter");
    }

    public void selectFirstItem() {
        page.click(".item-card:first-child");
    }

    public void clickCategory(String category) {
        page.click("button:has-text('" + category + "')");
    }

    public void clickRentableFilter() {
        page.click("button:has-text('Rentable')");
    }

    public boolean hasItems() {
        return page.locator(".item-card").count() > 0;
    }

    public boolean hasNoItemsMessage() {
        return page.isVisible("text=No items found");
    }
}
