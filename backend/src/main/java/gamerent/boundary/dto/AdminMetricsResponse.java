package gamerent.boundary.dto;

public class AdminMetricsResponse {
    private int totalAccounts;
    private int activeListings;
    private int totalBookings;
    private double monthlyRevenue;
    private int openIssues;

    public AdminMetricsResponse(int totalAccounts, int activeListings, int totalBookings, double monthlyRevenue, int openIssues) {
        this.totalAccounts = totalAccounts;
        this.activeListings = activeListings;
        this.totalBookings = totalBookings;
        this.monthlyRevenue = monthlyRevenue;
        this.openIssues = openIssues;
    }

    public int getTotalAccounts() { return totalAccounts; }
    public void setTotalAccounts(int totalAccounts) { this.totalAccounts = totalAccounts; }
    public int getActiveListings() { return activeListings; }
    public void setActiveListings(int activeListings) { this.activeListings = activeListings; }
    public int getTotalBookings() { return totalBookings; }
    public void setTotalBookings(int totalBookings) { this.totalBookings = totalBookings; }
    public double getMonthlyRevenue() { return monthlyRevenue; }
    public void setMonthlyRevenue(double monthlyRevenue) { this.monthlyRevenue = monthlyRevenue; }
    public int getOpenIssues() { return openIssues; }
    public void setOpenIssues(int openIssues) { this.openIssues = openIssues; }
}
