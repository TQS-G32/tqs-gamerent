Feature: Register Item
  As an owner
  I want to register a console or game with details
  So that renters can view and book it

  Scenario: Owner registers a new item
    Given a user "owner@example.com" exists as owner
    When the owner registers an item with name "Nintendo Switch" category "Console" and price 12.5
    Then the item "Nintendo Switch" should exist
