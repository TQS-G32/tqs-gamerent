Feature: Chat Between Renters and Owners
  As a renter
  I want to chat with item owners
  So that I can ask questions about items before renting

  Scenario: Renter initiates chat with owner about an item
    Given a user "renter@example.com" exists
    And a user "owner@example.com" exists
    And an item "PlayStation 5" exists owned by "owner@example.com"
    When the user "renter@example.com" starts a chat about item "PlayStation 5" with message "Is this available next week?"
    Then the chat should be created successfully
    And the chat should have item "PlayStation 5"
    And the chat should have 1 messages

  Scenario: Owner replies to renter's message
    Given a user "renter@example.com" exists
    And a user "owner@example.com" exists
    And an item "Xbox Series X" exists owned by "owner@example.com"
    And a chat exists between "renter@example.com" and "owner@example.com" about item "Xbox Series X"
    And the user "renter@example.com" sends message "Is this available?" in the chat
    When the user "owner@example.com" sends message "Yes, it's available!" in the chat
    Then the chat should have 2 messages

  Scenario: Renter cannot chat with themselves
    Given a user "owner@example.com" exists
    And an item "Nintendo Switch" exists owned by "owner@example.com"
    When the user "owner@example.com" tries to start a chat about item "Nintendo Switch"
    Then the chat creation should fail with error "Cannot create chat with yourself"

  Scenario: User retrieves all their chats
    Given a user "renter@example.com" exists
    And a user "owner1@example.com" exists
    And a user "owner2@example.com" exists
    And an item "Game A" exists owned by "owner1@example.com"
    And an item "Game B" exists owned by "owner2@example.com"
    And a chat exists between "renter@example.com" and "owner1@example.com" about item "Game A"
    And a chat exists between "renter@example.com" and "owner2@example.com" about item "Game B"
    When the user "renter@example.com" retrieves their chats
    Then the user should have 2 chats

  Scenario: User can only access their own chats
    Given a user "renter1@example.com" exists
    And a user "renter2@example.com" exists
    And a user "owner@example.com" exists
    And an item "Exclusive Game" exists owned by "owner@example.com"
    And a chat exists between "renter1@example.com" and "owner@example.com" about item "Exclusive Game"
    When the user "renter2@example.com" tries to access the chat
    Then the access should be denied

  Scenario: Messages are marked as read
    Given a user "renter@example.com" exists
    And a user "owner@example.com" exists
    And an item "Gaming PC" exists owned by "owner@example.com"
    And a chat exists between "renter@example.com" and "owner@example.com" about item "Gaming PC"
    And the user "owner@example.com" sends message "Hello!" in the chat
    When the user "renter@example.com" marks messages as read in the chat
    Then all messages from "owner@example.com" should be marked as read

  Scenario: Empty messages are rejected
    Given a user "renter@example.com" exists
    And a user "owner@example.com" exists
    And an item "VR Headset" exists owned by "owner@example.com"
    And a chat exists between "renter@example.com" and "owner@example.com" about item "VR Headset"
    When the user "renter@example.com" tries to send an empty message in the chat
    Then the message should be rejected with error "Message content cannot be empty"

  Scenario: User retrieves messages in a chat
    Given a user "renter@example.com" exists
    And a user "owner@example.com" exists
    And an item "Steam Deck" exists owned by "owner@example.com"
    And a chat exists between "renter@example.com" and "owner@example.com" about item "Steam Deck"
    And the user "renter@example.com" sends message "Is it in good condition?" in the chat
    And the user "owner@example.com" sends message "Yes, like new!" in the chat
    When the user "renter@example.com" retrieves messages from the chat
    Then the user should see 2 messages in order
