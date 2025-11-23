package gamerent.e2e.pages;

import com.microsoft.playwright.Page;

public class ItemPage {
    private final Page page;

    public ItemPage(Page page) {
        this.page = page;
    }

    public void book(String start, String end) {
        // Use robust locators based on labels
        page.getByLabel("From").fill(start);
        page.getByLabel("To").fill(end);
        page.click("button:text('Request Booking')");
    }

    public boolean isBookingSuccessful() {
        // Increased timeout for success message appearance
        return page.isVisible("text=Booking sent!");
    }
}
