Feature: Search Items
  As a renter
  I want to search for items
  So that I can find what I need

  Scenario: Search items by name
    Given the following items exist:
      | name        | category | price |
      | PS5 Console | Console  | 20.0  |
      | Xbox One    | Console  | 15.0  |
    When I search for "PS5"
    Then I should find "PS5 Console"
    And I should not find "Xbox One"


