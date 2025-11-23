Feature: Book Item
  As a renter
  I want to book an item
  So that I can use it

  Scenario: Renter books an item
    Given a user "renter@example.com" exists
    And an item "Switch" exists owned by "owner@example.com"
    When the user "renter@example.com" books "Switch" for tomorrow
    Then the booking status should be "PENDING"


