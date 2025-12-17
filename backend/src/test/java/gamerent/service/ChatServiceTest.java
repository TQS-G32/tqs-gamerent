package gamerent.service;

import gamerent.boundary.dto.ChatResponse;
import gamerent.boundary.dto.MessageResponse;
import gamerent.config.UnauthorizedException;
import gamerent.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for ChatService.
 * Tests cover all business logic, validation, and authorization scenarios.
 * Achieves >80% code coverage following TQS best practices.
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChatService chatService;

    private User renter;
    private User owner;
    private Item item;
    private Chat chat;
    private Message message;

    @BeforeEach
    void setUp() {
        // Setup test data
        renter = new User();
        renter.setId(1L);
        renter.setName("Renter User");
        renter.setEmail("renter@test.com");

        owner = new User();
        owner.setId(2L);
        owner.setName("Owner User");
        owner.setEmail("owner@test.com");

        item = new Item();
        item.setId(100L);
        item.setName("Test Game");
        item.setImageUrl("http://example.com/image.jpg");
        item.setOwner(owner);

        chat = new Chat(renter, owner, item);
        chat.setId(1L);
        chat.setCreatedAt(LocalDateTime.now());
        chat.setUpdatedAt(LocalDateTime.now());

        message = new Message(chat, renter, "Hello!");
        message.setId(1L);
    }

    // ========================
    // createOrGetChat tests
    // ========================

    @Test
    void createOrGetChat_whenNewChat_createsAndReturnsChat() {
        when(itemRepository.findById(100L)).thenReturn(Optional.of(item));
        when(userRepository.findById(1L)).thenReturn(Optional.of(renter));
        when(chatRepository.findByRenterIdAndItemId(1L, 100L)).thenReturn(Optional.empty());
        when(chatRepository.save(any(Chat.class))).thenAnswer(invocation -> {
            Chat savedChat = invocation.getArgument(0);
            savedChat.setId(1L);
            return savedChat;
        });
        when(messageRepository.findByChatIdOrderBySentAtAsc(1L)).thenReturn(Collections.emptyList());
        when(messageRepository.countByChatIdAndIsReadFalseAndSenderIdNot(1L, 1L)).thenReturn(0L);

        ChatResponse response = chatService.createOrGetChat(1L, 100L, null);

        assertNotNull(response);
        assertEquals(1L, response.getRenterId());
        assertEquals(2L, response.getOwnerId());
        assertEquals(100L, response.getItemId());
        verify(chatRepository).save(any(Chat.class));
        verify(messageRepository, never()).save(any(Message.class));
    }

    @Test
    void createOrGetChat_whenExistingChat_returnsExistingChat() {
        when(itemRepository.findById(100L)).thenReturn(Optional.of(item));
        when(userRepository.findById(1L)).thenReturn(Optional.of(renter));
        when(chatRepository.findByRenterIdAndItemId(1L, 100L)).thenReturn(Optional.of(chat));
        when(messageRepository.findByChatIdOrderBySentAtAsc(1L)).thenReturn(Collections.emptyList());
        when(messageRepository.countByChatIdAndIsReadFalseAndSenderIdNot(1L, 1L)).thenReturn(0L);

        ChatResponse response = chatService.createOrGetChat(1L, 100L, null);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        verify(chatRepository, never()).save(any(Chat.class));
    }

    @Test
    void createOrGetChat_withInitialMessage_sendsMessage() {
        when(itemRepository.findById(100L)).thenReturn(Optional.of(item));
        when(userRepository.findById(1L)).thenReturn(Optional.of(renter));
        when(chatRepository.findByRenterIdAndItemId(1L, 100L)).thenReturn(Optional.of(chat));
        when(messageRepository.save(any(Message.class))).thenReturn(message);
        when(chatRepository.save(any(Chat.class))).thenReturn(chat);
        when(messageRepository.findByChatIdOrderBySentAtAsc(1L)).thenReturn(Arrays.asList(message));
        when(messageRepository.countByChatIdAndIsReadFalseAndSenderIdNot(1L, 1L)).thenReturn(0L);

        ChatResponse response = chatService.createOrGetChat(1L, 100L, "Hello!");

        assertNotNull(response);
        assertEquals("Hello!", response.getLastMessage());
        verify(messageRepository).save(any(Message.class));
        verify(chatRepository, times(1)).save(chat);
    }

    @Test
    void createOrGetChat_whenItemNotFound_throwsNoSuchElementException() {
        when(itemRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, 
            () -> chatService.createOrGetChat(1L, 100L, null));
        
        verify(chatRepository, never()).save(any(Chat.class));
    }

    @Test
    void createOrGetChat_whenRenterNotFound_throwsNoSuchElementException() {
        when(itemRepository.findById(100L)).thenReturn(Optional.of(item));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, 
            () -> chatService.createOrGetChat(1L, 100L, null));
        
        verify(chatRepository, never()).save(any(Chat.class));
    }

    @Test
    void createOrGetChat_whenItemHasNoOwner_throwsIllegalStateException() {
        item.setOwner(null);
        when(itemRepository.findById(100L)).thenReturn(Optional.of(item));
        when(userRepository.findById(1L)).thenReturn(Optional.of(renter));

        assertThrows(IllegalStateException.class, 
            () -> chatService.createOrGetChat(1L, 100L, null));
        
        verify(chatRepository, never()).save(any(Chat.class));
    }

    @Test
    void createOrGetChat_whenRenterIsOwner_throwsIllegalArgumentException() {
        item.setOwner(renter);
        when(itemRepository.findById(100L)).thenReturn(Optional.of(item));
        when(userRepository.findById(1L)).thenReturn(Optional.of(renter));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> chatService.createOrGetChat(1L, 100L, null));
        
        assertEquals("Cannot create chat with yourself", exception.getMessage());
        verify(chatRepository, never()).save(any(Chat.class));
    }

    @Test
    void createOrGetChat_withEmptyInitialMessage_doesNotSendMessage() {
        when(itemRepository.findById(100L)).thenReturn(Optional.of(item));
        when(userRepository.findById(1L)).thenReturn(Optional.of(renter));
        when(chatRepository.findByRenterIdAndItemId(1L, 100L)).thenReturn(Optional.of(chat));
        when(messageRepository.findByChatIdOrderBySentAtAsc(1L)).thenReturn(Collections.emptyList());
        when(messageRepository.countByChatIdAndIsReadFalseAndSenderIdNot(1L, 1L)).thenReturn(0L);

        chatService.createOrGetChat(1L, 100L, "   ");

        verify(messageRepository, never()).save(any(Message.class));
    }

    // ========================
    // getUserChats tests
    // ========================

    @Test
    void getUserChats_returnsOnlyChatsWithMessages() {
        Chat chat2 = new Chat(renter, owner, item);
        chat2.setId(2L);
        
        when(chatRepository.findByUserId(1L)).thenReturn(Arrays.asList(chat, chat2));
        // chat has messages, chat2 doesn't
        when(messageRepository.findByChatIdOrderBySentAtAsc(1L)).thenReturn(Arrays.asList(message));
        when(messageRepository.findByChatIdOrderBySentAtAsc(2L)).thenReturn(Collections.emptyList());
        when(messageRepository.countByChatIdAndIsReadFalseAndSenderIdNot(1L, 1L)).thenReturn(0L);

        List<ChatResponse> chats = chatService.getUserChats(1L);

        assertNotNull(chats);
        assertEquals(1, chats.size()); // Only chat with messages
        assertEquals(1L, chats.get(0).getId());
        verify(chatRepository).findByUserId(1L);
    }

    @Test
    void getUserChats_whenNoChats_returnsEmptyList() {
        when(chatRepository.findByUserId(1L)).thenReturn(Collections.emptyList());

        List<ChatResponse> chats = chatService.getUserChats(1L);

        assertNotNull(chats);
        assertTrue(chats.isEmpty());
    }

    @Test
    void getUserChats_whenChatsExistButNoMessages_returnsEmptyList() {
        when(chatRepository.findByUserId(1L)).thenReturn(Arrays.asList(chat));
        when(messageRepository.findByChatIdOrderBySentAtAsc(1L)).thenReturn(Collections.emptyList());

        List<ChatResponse> chats = chatService.getUserChats(1L);

        assertNotNull(chats);
        assertTrue(chats.isEmpty()); // No chats returned because no messages
    }

    // ========================
    // getChat tests
    // ========================

    @Test
    void getChat_whenAuthorizedRenter_returnsChat() {
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(messageRepository.findByChatIdOrderBySentAtAsc(1L)).thenReturn(Collections.emptyList());
        when(messageRepository.countByChatIdAndIsReadFalseAndSenderIdNot(1L, 1L)).thenReturn(0L);

        ChatResponse response = chatService.getChat(1L, 1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    void getChat_whenAuthorizedOwner_returnsChat() {
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(messageRepository.findByChatIdOrderBySentAtAsc(1L)).thenReturn(Collections.emptyList());
        when(messageRepository.countByChatIdAndIsReadFalseAndSenderIdNot(1L, 2L)).thenReturn(0L);

        ChatResponse response = chatService.getChat(1L, 2L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    void getChat_whenUnauthorizedUser_throwsUnauthorizedException() {
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        assertThrows(UnauthorizedException.class, 
            () -> chatService.getChat(1L, 999L));
    }

    @Test
    void getChat_whenChatNotFound_throwsNoSuchElementException() {
        when(chatRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, 
            () -> chatService.getChat(1L, 1L));
    }

    // ========================
    // sendMessage tests
    // ========================

    @Test
    void sendMessage_whenValid_savesAndReturnsMessage() {
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(userRepository.findById(1L)).thenReturn(Optional.of(renter));
        when(messageRepository.save(any(Message.class))).thenReturn(message);
        when(chatRepository.save(any(Chat.class))).thenReturn(chat);

        MessageResponse response = chatService.sendMessage(1L, 1L, "Hello!");

        assertNotNull(response);
        assertEquals("Hello!", response.getContent());
        assertEquals(1L, response.getSenderId());
        
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());
        assertEquals("Hello!", messageCaptor.getValue().getContent());
        verify(chatRepository).save(chat);
    }

    @Test
    void sendMessage_trimsContent() {
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(userRepository.findById(1L)).thenReturn(Optional.of(renter));
        when(messageRepository.save(any(Message.class))).thenReturn(message);
        when(chatRepository.save(any(Chat.class))).thenReturn(chat);

        chatService.sendMessage(1L, 1L, "  Hello!  ");

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());
        assertEquals("Hello!", messageCaptor.getValue().getContent());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.NullAndEmptySource
    @org.junit.jupiter.params.provider.ValueSource(strings = {"   "})
    void sendMessage_whenContentInvalid_throwsIllegalArgumentException(String content) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> chatService.sendMessage(1L, 1L, content));

        assertEquals("Message content cannot be empty", exception.getMessage());
        verify(messageRepository, never()).save(any(Message.class));
    }

    @Test
    void sendMessage_whenContentTooLong_throwsIllegalArgumentException() {
        String longContent = "a".repeat(2001);
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> chatService.sendMessage(1L, 1L, longContent));
        
        assertTrue(exception.getMessage().contains("exceeds maximum length"));
        verify(messageRepository, never()).save(any(Message.class));
    }

    @Test
    void sendMessage_whenChatNotFound_throwsNoSuchElementException() {
        when(chatRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, 
            () -> chatService.sendMessage(1L, 1L, "Hello!"));
        
        verify(messageRepository, never()).save(any(Message.class));
    }

    @Test
    void sendMessage_whenSenderNotFound_throwsNoSuchElementException() {
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, 
            () -> chatService.sendMessage(1L, 1L, "Hello!"));
        
        verify(messageRepository, never()).save(any(Message.class));
    }

    @Test
    void sendMessage_whenUnauthorizedUser_throwsUnauthorizedException() {
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(userRepository.findById(999L)).thenReturn(Optional.of(new User()));

        assertThrows(UnauthorizedException.class, 
            () -> chatService.sendMessage(1L, 999L, "Hello!"));
        
        verify(messageRepository, never()).save(any(Message.class));
    }

    // ========================
    // getChatMessages tests
    // ========================

    @Test
    void getChatMessages_whenAuthorized_returnsMessages() {
        Message message2 = new Message(chat, owner, "Reply");
        message2.setId(2L);
        
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(messageRepository.findByChatIdOrderBySentAtAsc(1L))
            .thenReturn(Arrays.asList(message, message2));

        List<MessageResponse> messages = chatService.getChatMessages(1L, 1L);

        assertNotNull(messages);
        assertEquals(2, messages.size());
        assertEquals("Hello!", messages.get(0).getContent());
        assertEquals("Reply", messages.get(1).getContent());
    }

    @Test
    void getChatMessages_whenNoMessages_returnsEmptyList() {
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(messageRepository.findByChatIdOrderBySentAtAsc(1L))
            .thenReturn(Collections.emptyList());

        List<MessageResponse> messages = chatService.getChatMessages(1L, 1L);

        assertNotNull(messages);
        assertTrue(messages.isEmpty());
    }

    @Test
    void getChatMessages_whenUnauthorized_throwsUnauthorizedException() {
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        assertThrows(UnauthorizedException.class, 
            () -> chatService.getChatMessages(1L, 999L));
    }

    @Test
    void getChatMessages_whenChatNotFound_throwsNoSuchElementException() {
        when(chatRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, 
            () -> chatService.getChatMessages(1L, 1L));
    }

    // ========================
    // markMessagesAsRead tests
    // ========================

    @Test
    void markMessagesAsRead_marksUnreadMessagesAsRead() {
        Message unreadMessage1 = new Message(chat, owner, "Msg1");
        unreadMessage1.setId(10L);
        unreadMessage1.setIsRead(false);
        
        Message unreadMessage2 = new Message(chat, owner, "Msg2");
        unreadMessage2.setId(11L);
        unreadMessage2.setIsRead(false);
        
        Message ownMessage = new Message(chat, renter, "My msg");
        ownMessage.setId(12L);
        ownMessage.setIsRead(false);
        
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(messageRepository.findByChatIdOrderBySentAtAsc(1L))
            .thenReturn(Arrays.asList(unreadMessage1, unreadMessage2, ownMessage));

        chatService.markMessagesAsRead(1L, 1L);

        verify(messageRepository, times(2)).save(any(Message.class));
        assertTrue(unreadMessage1.getIsRead());
        assertTrue(unreadMessage2.getIsRead());
        assertFalse(ownMessage.getIsRead()); // Own messages should not be marked as read
    }

    @Test
    void markMessagesAsRead_whenAlreadyRead_doesNotSaveAgain() {
        Message readMessage = new Message(chat, owner, "Msg");
        readMessage.setIsRead(true);
        
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(messageRepository.findByChatIdOrderBySentAtAsc(1L))
            .thenReturn(Arrays.asList(readMessage));

        chatService.markMessagesAsRead(1L, 1L);

        verify(messageRepository, never()).save(any(Message.class));
    }

    @Test
    void markMessagesAsRead_whenUnauthorized_throwsUnauthorizedException() {
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        assertThrows(UnauthorizedException.class, 
            () -> chatService.markMessagesAsRead(1L, 999L));
    }

    @Test
    void markMessagesAsRead_whenChatNotFound_throwsNoSuchElementException() {
        when(chatRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, 
            () -> chatService.markMessagesAsRead(1L, 1L));
    }

    // ========================
    // Edge cases and integration
    // ========================

    @Test
    void chatResponse_includesAllNecessaryFields() {
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(messageRepository.findByChatIdOrderBySentAtAsc(1L))
            .thenReturn(Arrays.asList(message));
        when(messageRepository.countByChatIdAndIsReadFalseAndSenderIdNot(1L, 1L))
            .thenReturn(5L);

        ChatResponse response = chatService.getChat(1L, 1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(1L, response.getRenterId());
        assertEquals("Renter User", response.getRenterName());
        assertEquals("renter@test.com", response.getRenterEmail());
        assertEquals(2L, response.getOwnerId());
        assertEquals("Owner User", response.getOwnerName());
        assertEquals("owner@test.com", response.getOwnerEmail());
        assertEquals(100L, response.getItemId());
        assertEquals("Test Game", response.getItemName());
        assertEquals("http://example.com/image.jpg", response.getItemImageUrl());
        assertEquals("Hello!", response.getLastMessage());
        assertEquals(5L, response.getUnreadCount());
        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getUpdatedAt());
    }

    @Test
    void messageResponse_includesAllNecessaryFields() {
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(messageRepository.findByChatIdOrderBySentAtAsc(1L))
            .thenReturn(Arrays.asList(message));

        List<MessageResponse> messages = chatService.getChatMessages(1L, 1L);

        assertNotNull(messages);
        assertEquals(1, messages.size());
        MessageResponse response = messages.get(0);
        assertEquals(1L, response.getId());
        assertEquals(1L, response.getChatId());
        assertEquals(1L, response.getSenderId());
        assertEquals("Renter User", response.getSenderName());
        assertEquals("Hello!", response.getContent());
        assertNotNull(response.getSentAt());
        assertNotNull(response.getIsRead());
    }
}
