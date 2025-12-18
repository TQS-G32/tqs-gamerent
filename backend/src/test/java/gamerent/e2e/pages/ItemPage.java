package gamerent.e2e.pages;

import com.microsoft.playwright.Page;

public class ItemPage {
    private final Page page;

    public ItemPage(Page page) {
        this.page = page;
    }

    public void book(String start, String end) {
        page.getByLabel("Start Date").fill(start);
        page.getByLabel("End Date").fill(end);
        page.click("button:has-text('Request Booking')");
    }

    public boolean isBookingSuccessful() {
        return page.isVisible("text=Booking request sent") || page.isVisible("text=Booking sent") || page.isVisible("text=Booking requested");
    }

    public boolean hasBookingControls() {
        return page.isVisible("button:has-text('Request Booking')") || page.isVisible("input[type=date]");
    }

    public boolean hasItemDetails() {
        return page.locator("h1").count() > 0;
    }

    public boolean hasOwnerCard() {
        return page.isVisible("text=Member for");
    }

    public boolean hasAvailableRentalsSection() {
        return page.isVisible("text=📋 Available Rentals");
    }

    public void clickSaveSettings() {
        page.click("button:has-text('💾 Save Changes')");
    }

    public boolean hasOwnerConfiguration() {
        return page.isVisible("text=⚙️ Configure Your Listing");
    }

    public void toggleAvailableForRent() {
        page.click("input[type='checkbox']");
    }

    public boolean hasSuccessMessage() {
        return page.isVisible("text=✓");
    }
}
