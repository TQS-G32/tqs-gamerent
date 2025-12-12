Feature: Manage Bookings
  As an owner
  I want to view and manage my upcoming bookings
  So I can prepare the items

  Scenario: Owner views upcoming bookings
    Given an owner "owner@example.com" with an item "PS4" exists
    And a future booking exists for item "PS4" by user "renter@example.com"
    When the owner checks their bookings
    Then they should see a booking for item "PS4"
