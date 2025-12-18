package gamerent.service;

import gamerent.boundary.dto.ChatResponse;
import gamerent.boundary.dto.MessageResponse;
import gamerent.config.UnauthorizedException;
import gamerent.data.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service layer for managing chats between renters and item owners.
 * Implements business logic, validation, and authorization checks.
 */
@Service
public class ChatService {
    private static final Logger logger = Logger.getLogger(ChatService.class.getName());
    private static final String CHAT_NOT_FOUND = "Chat not found";
    
    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    public ChatService(ChatRepository chatRepository, MessageRepository messageRepository, 
                      ItemRepository itemRepository, UserRepository userRepository) {
        this.chatRepository = chatRepository;
        this.messageRepository = messageRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
    }

    /**
     * Create or retrieve a chat for a specific item.
     * Validates that the user is not the owner of the item.
     * 
     * @param renterId The ID of the user initiating the chat (renter)
     * @param itemId The ID of the item being discussed
     * @param initialMessage Optional initial message to send
     * @return ChatResponse containing the chat details
     * @throws NoSuchElementException if item or user not found
     * @throws IllegalArgumentException if user tries to chat with themselves
     */
    @Transactional
    public ChatResponse createOrGetChat(Long renterId, Long itemId, String initialMessage) {
        logger.log(Level.INFO, "Creating or getting chat for renter {0} and item {1}", 
                  new Object[]{renterId, itemId});
        
        // Validate item exists
        Item item = itemRepository.findById(itemId)
            .orElseThrow(() -> new NoSuchElementException("Item not found"));
        
        // Validate renter exists
        User renter = userRepository.findById(renterId)
            .orElseThrow(() -> new NoSuchElementException("User not found"));
        
        // Validate item has an owner
        if (item.getOwner() == null) {
            throw new IllegalStateException("Item does not have an owner");
        }
        
        // Prevent user from chatting with themselves
        if (item.getOwner().getId().equals(renterId)) {
            throw new IllegalArgumentException("Cannot create chat with yourself");
        }
        
        // Check if chat already exists
        Chat chat = chatRepository.findByRenterIdAndItemId(renterId, itemId)
            .orElseGet(() -> {
                // Create new chat
                Chat newChat = new Chat(renter, item.getOwner(), item);
                Chat savedChat = chatRepository.save(newChat);
                logger.log(Level.INFO, "Created new chat with ID {0}", savedChat.getId());
                return savedChat;
            });
        
        // Send initial message if provided
        if (initialMessage != null && !initialMessage.trim().isEmpty()) {
            Message message = new Message(chat, renter, initialMessage.trim());
            messageRepository.save(message);
            chat.setUpdatedAt(LocalDateTime.now());
            chatRepository.save(chat);
            logger.log(Level.INFO, "Sent initial message in chat {0}", chat.getId());
        }
        
        return convertToChatResponse(chat, renterId);
    }

    /**
     * Get all chats for a user (both as renter and owner).
     * Only returns chats that have at least one message.
     * 
     * @param userId The ID of the user
     * @return List of ChatResponse objects
     */
    @Transactional(readOnly = true)
    public List<ChatResponse> getUserChats(Long userId) {
        logger.log(Level.INFO, "Retrieving chats for user {0}", userId);
        
        List<Chat> chats = chatRepository.findByUserId(userId);
        
        return chats.stream()
            .filter(chat -> {
                // Only include chats that have at least one message
                List<Message> messages = messageRepository.findByChatIdOrderBySentAtAsc(chat.getId());
                return !messages.isEmpty();
            })
            .map(chat -> convertToChatResponse(chat, userId))
            .toList();
    }

    /**
     * Get a specific chat by ID.
     * Validates that the user is a participant in the chat.
     * 
     * @param chatId The ID of the chat
     * @param userId The ID of the requesting user
     * @return ChatResponse containing the chat details
     * @throws NoSuchElementException if chat not found
     * @throws UnauthorizedException if user is not a participant
     */
    @Transactional(readOnly = true)
    public ChatResponse getChat(Long chatId, Long userId) {
        logger.log(Level.INFO, "Retrieving chat {0} for user {1}", 
                  new Object[]{chatId, userId});
        Chat chat = chatRepository.findById(chatId)
            .orElseThrow(() -> new NoSuchElementException(CHAT_NOT_FOUND));        
        // Validate user is a participant
        if (!chat.getRenter().getId().equals(userId) && !chat.getOwner().getId().equals(userId)) {
            logger.log(Level.WARNING, "User {0} attempted to access unauthorized chat {1}", 
                      new Object[]{userId, chatId});
            throw new UnauthorizedException("You are not a participant in this chat");
        }
        
        return convertToChatResponse(chat, userId);
    }

    /**
     * Send a message in a chat.
     * Validates that the user is a participant and updates the chat timestamp.
     * 
     * @param chatId The ID of the chat
     * @param senderId The ID of the message sender
     * @param content The message content
     * @return MessageResponse containing the message details
     * @throws NoSuchElementException if chat or sender not found
     * @throws UnauthorizedException if user is not a participant
     * @throws IllegalArgumentException if content is empty
     */
    @Transactional
    public MessageResponse sendMessage(Long chatId, Long senderId, String content) {
        logger.log(Level.INFO, "Sending message in chat {0} from user {1}", 
                  new Object[]{chatId, senderId});
        
        // Validate content
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }
        
        if (content.length() > 2000) {
            throw new IllegalArgumentException("Message content exceeds maximum length of 2000 characters");
        }
        
        // Validate chat exists
        Chat chat = chatRepository.findById(chatId)
            .orElseThrow(() -> new NoSuchElementException(CHAT_NOT_FOUND));
        
        // Validate sender exists
        User sender = userRepository.findById(senderId)
            .orElseThrow(() -> new NoSuchElementException("User not found"));
        
        // Validate sender is a participant
        if (!chat.getRenter().getId().equals(senderId) && !chat.getOwner().getId().equals(senderId)) {
            logger.log(Level.WARNING, "User {0} attempted to send message in unauthorized chat {1}", 
                      new Object[]{senderId, chatId});
            throw new UnauthorizedException("You are not a participant in this chat");
        }
        
        // Create and save message
        Message message = new Message(chat, sender, content.trim());
        Message savedMessage = messageRepository.save(message);
        
        // Update chat timestamp
        chat.setUpdatedAt(LocalDateTime.now());
        chatRepository.save(chat);
        
        logger.log(Level.INFO, "Message {0} sent successfully", savedMessage.getId());
        
        return convertToMessageResponse(savedMessage);
    }

    /**
     * Get all messages in a chat.
     * Validates that the user is a participant.
     * 
     * @param chatId The ID of the chat
     * @param userId The ID of the requesting user
     * @return List of MessageResponse objects
     * @throws NoSuchElementException if chat not found
     * @throws UnauthorizedException if user is not a participant
     */
    @Transactional(readOnly = true)
    public List<MessageResponse> getChatMessages(Long chatId, Long userId) {
        logger.log(Level.INFO, "Retrieving messages for chat {0} by user {1}", 
                  new Object[]{chatId, userId});
        
        // Validate chat exists
        Chat chat = chatRepository.findById(chatId)
            .orElseThrow(() -> new NoSuchElementException(CHAT_NOT_FOUND));
        
        // Validate user is a participant
        if (!chat.getRenter().getId().equals(userId) && !chat.getOwner().getId().equals(userId)) {
            logger.log(Level.WARNING, "User {0} attempted to access messages in unauthorized chat {1}", 
                      new Object[]{userId, chatId});
            throw new UnauthorizedException("You are not a participant in this chat");
        }
        
        List<Message> messages = messageRepository.findByChatIdOrderBySentAtAsc(chatId);
        
        return messages.stream()
            .map(this::convertToMessageResponse)
            .toList();
    }

    /**
     * Mark messages as read by the current user.
     * 
     * @param chatId The ID of the chat
     * @param userId The ID of the user marking messages as read
     * @throws NoSuchElementException if chat not found
     * @throws UnauthorizedException if user is not a participant
     */
    @Transactional
    public void markMessagesAsRead(Long chatId, Long userId) {
        logger.log(Level.INFO, "Marking messages as read in chat {0} for user {1}", 
                  new Object[]{chatId, userId});
        
        // Validate chat exists and user is participant
        Chat chat = chatRepository.findById(chatId)
            .orElseThrow(() -> new NoSuchElementException(CHAT_NOT_FOUND));
        
        if (!chat.getRenter().getId().equals(userId) && !chat.getOwner().getId().equals(userId)) {
            throw new UnauthorizedException("You are not a participant in this chat");
        }
        
        // Get all unread messages not sent by the current user
        List<Message> messages = messageRepository.findByChatIdOrderBySentAtAsc(chatId);
        messages.stream()
            .filter(msg -> !msg.getSender().getId().equals(userId) && !msg.getIsRead())
            .forEach(msg -> {
                msg.setIsRead(true);
                messageRepository.save(msg);
            });
        
        logger.log(Level.INFO, "Messages marked as read in chat {0}", chatId);
    }

    /**
     * Convert Chat entity to ChatResponse DTO.
     */
    private ChatResponse convertToChatResponse(Chat chat, Long currentUserId) {
        List<Message> messages = messageRepository.findByChatIdOrderBySentAtAsc(chat.getId());
        String lastMessage = messages.isEmpty() ? null : 
            messages.get(messages.size() - 1).getContent();
        
        Long unreadCount = messageRepository.countByChatIdAndIsReadFalseAndSenderIdNot(
            chat.getId(), currentUserId);
        
        return new ChatResponse(
            chat.getId(),
            chat.getRenter().getId(),
            chat.getRenter().getName(),
            chat.getRenter().getEmail(),
            chat.getOwner().getId(),
            chat.getOwner().getName(),
            chat.getOwner().getEmail(),
            chat.getItem().getId(),
            chat.getItem().getName(),
            chat.getItem().getImageUrl(),
            chat.getCreatedAt(),
            chat.getUpdatedAt(),
            lastMessage,
            unreadCount
        );
    }

    /**
     * Convert Message entity to MessageResponse DTO.
     */
    private MessageResponse convertToMessageResponse(Message message) {
        return new MessageResponse(
            message.getId(),
            message.getChat().getId(),
            message.getSender().getId(),
            message.getSender().getName(),
            message.getContent(),
            message.getSentAt(),
            message.getIsRead()
        );
    }
}
