package gamerent.bdd.steps;

import gamerent.boundary.dto.ChatResponse;
import gamerent.boundary.dto.MessageResponse;
import gamerent.config.UnauthorizedException;
import gamerent.data.*;
import gamerent.service.ChatService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cucumber step definitions for chat feature BDD tests.
 */
@SpringBootTest
public class ChatSteps {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ChatService chatService;

    private Chat currentChat;
    private ChatResponse currentChatResponse;
    private List<ChatResponse> userChats;
    private List<MessageResponse> chatMessages;
    private Exception thrownException;

    /**
     * Helper method to create or retrieve a user.
     */
    private User createOrGetUser(String email) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User user = new User();
            user.setEmail(email);
            user.setName(email.split("@")[0]);
            user.setPassword("password");
            user.setRole(email.contains("owner") ? "OWNER" : "RENTER");
            return userRepository.save(user);
        });
    }

    /**
     * Helper method to retrieve or create user by email.
     * This ensures users are available even if the @Given step is defined elsewhere.
     */
    private User getUser(String email) {
        return createOrGetUser(email);
    }

    /**
     * Helper method to retrieve item by name.
     */
    private Item getItem(String itemName) {
        return itemRepository.findAll().stream()
            .filter(i -> i.getName().equals(itemName))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Item not found: " + itemName));
    }

    // Note: "a user {string} exists" and "an item {string} exists owned by {string}" 
    // steps are defined in BookingSteps to avoid duplication

    @Given("a chat exists between {string} and {string} about item {string}")
    public void a_chat_exists_between_and_about_item(String renterEmail, String ownerEmail, String itemName) {
        User renter = getUser(renterEmail);
        Item item = getItem(itemName);
        
        currentChatResponse = chatService.createOrGetChat(renter.getId(), item.getId(), null);
        currentChat = chatRepository.findById(currentChatResponse.getId())
            .orElseThrow(() -> new RuntimeException("Chat not found"));
    }

    @When("the user {string} starts a chat about item {string} with message {string}")
    public void the_user_starts_a_chat_about_item_with_message(String renterEmail, String itemName, String message) {
        User renter = getUser(renterEmail);
        Item item = getItem(itemName);
        
        try {
            currentChatResponse = chatService.createOrGetChat(renter.getId(), item.getId(), message);
            currentChat = chatRepository.findById(currentChatResponse.getId())
                .orElseThrow(() -> new RuntimeException("Chat not found"));
            thrownException = null;
        } catch (Exception e) {
            thrownException = e;
        }
    }

    @When("the user {string} tries to start a chat about item {string}")
    public void the_user_tries_to_start_a_chat_about_item(String userEmail, String itemName) {
        User user = getUser(userEmail);
        Item item = getItem(itemName);
        
        try {
            currentChatResponse = chatService.createOrGetChat(user.getId(), item.getId(), null);
            thrownException = null;
        } catch (Exception e) {
            thrownException = e;
        }
    }

    @When("the user {string} sends message {string} in the chat")
    public void the_user_sends_message_in_the_chat(String userEmail, String messageContent) {
        User user = getUser(userEmail);
        
        try {
            chatService.sendMessage(currentChat.getId(), user.getId(), messageContent);
            thrownException = null;
        } catch (Exception e) {
            thrownException = e;
        }
    }

    @When("the user {string} tries to send an empty message in the chat")
    public void the_user_tries_to_send_an_empty_message_in_the_chat(String userEmail) {
        User user = getUser(userEmail);
        
        try {
            chatService.sendMessage(currentChat.getId(), user.getId(), "   ");
            thrownException = null;
        } catch (Exception e) {
            thrownException = e;
        }
    }

    @When("the user {string} retrieves their chats")
    public void the_user_retrieves_their_chats(String userEmail) {
        User user = getUser(userEmail);
        userChats = chatService.getUserChats(user.getId());
    }

    @When("the user {string} tries to access the chat")
    public void the_user_tries_to_access_the_chat(String userEmail) {
        User user = getUser(userEmail);
        
        try {
            currentChatResponse = chatService.getChat(currentChat.getId(), user.getId());
            thrownException = null;
        } catch (Exception e) {
            thrownException = e;
        }
    }

    @When("the user {string} marks messages as read in the chat")
    public void the_user_marks_messages_as_read_in_the_chat(String userEmail) {
        User user = getUser(userEmail);
        chatService.markMessagesAsRead(currentChat.getId(), user.getId());
    }

    @When("the user {string} retrieves messages from the chat")
    public void the_user_retrieves_messages_from_the_chat(String userEmail) {
        User user = getUser(userEmail);
        chatMessages = chatService.getChatMessages(currentChat.getId(), user.getId());
    }

    @Then("the chat should be created successfully")
    public void the_chat_should_be_created_successfully() {
        assertNotNull(currentChatResponse);
        assertNotNull(currentChatResponse.getId());
        assertNull(thrownException);
    }

    @Then("the chat should have item {string}")
    public void the_chat_should_have_item(String itemName) {
        assertEquals(itemName, currentChatResponse.getItemName());
    }

    @Then("the chat should have {int} messages")
    public void the_chat_should_have_messages(Integer messageCount) {
        List<Message> messages = messageRepository.findByChatIdOrderBySentAtAsc(currentChat.getId());
        assertEquals(messageCount, messages.size());
    }

    @Then("the chat creation should fail with error {string}")
    public void the_chat_creation_should_fail_with_error(String errorMessage) {
        assertNotNull(thrownException);
        assertTrue(thrownException.getMessage().contains(errorMessage));
    }

    @Then("the user should have {int} chats")
    public void the_user_should_have_chats(Integer chatCount) {
        assertNotNull(userChats);
        assertEquals(chatCount, userChats.size());
    }

    @Then("the access should be denied")
    public void the_access_should_be_denied() {
        assertNotNull(thrownException);
        assertTrue(thrownException instanceof UnauthorizedException);
    }

    @Then("all messages from {string} should be marked as read")
    public void all_messages_from_should_be_marked_as_read(String senderEmail) {
        User sender = getUser(senderEmail);
        List<Message> messages = messageRepository.findByChatIdOrderBySentAtAsc(currentChat.getId());
        
        messages.stream()
            .filter(m -> m.getSender().getId().equals(sender.getId()))
            .forEach(m -> assertTrue(m.getIsRead(), "Message should be marked as read"));
    }

    @Then("the message should be rejected with error {string}")
    public void the_message_should_be_rejected_with_error(String errorMessage) {
        assertNotNull(thrownException);
        assertTrue(thrownException instanceof IllegalArgumentException);
        assertTrue(thrownException.getMessage().contains(errorMessage));
    }

    @Then("the user should see {int} messages in order")
    public void the_user_should_see_messages_in_order(Integer messageCount) {
        assertNotNull(chatMessages);
        assertEquals(messageCount, chatMessages.size());
        
        // Verify messages are ordered by sentAt (ascending)
        for (int i = 1; i < chatMessages.size(); i++) {
            assertTrue(
                chatMessages.get(i-1).getSentAt().isBefore(chatMessages.get(i).getSentAt()) ||
                chatMessages.get(i-1).getSentAt().isEqual(chatMessages.get(i).getSentAt()),
                "Messages should be ordered by sent time"
            );
        }
    }
}
