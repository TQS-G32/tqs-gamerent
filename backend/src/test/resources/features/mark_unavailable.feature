Feature: Mark Item Unavailable
  As an owner
  I want to mark my item as temporarily unavailable
  So I can manage maintenance schedules

  Scenario: Owner marks item as unavailable and sets minimum rental days
    Given an owner "owner@example.com" with an item "PS2" exists
    When the owner marks item "PS2" unavailable and sets min rental days to 3
    Then the item "PS2" should be unavailable and have minimum rental days 3
