package gamerent.boundary;

import gamerent.boundary.dto.ChatRequest;
import gamerent.boundary.dto.ChatResponse;
import gamerent.boundary.dto.MessageRequest;
import gamerent.boundary.dto.MessageResponse;
import gamerent.config.UnauthorizedException;
import gamerent.service.ChatService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/chats")
@CrossOrigin(origins = "*")
public class ChatController {
    private static final Logger logger = Logger.getLogger(ChatController.class.getName());
    private static final String USER_ID_KEY = "userId";
    private static final String NOT_AUTHENTICATED_MSG = "Not authenticated";
    private static final String CHAT_NOT_FOUND = "Chat not found";

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<Object> createOrGetChat(@RequestBody ChatRequest request, 
                                                   HttpServletRequest httpRequest) {
        logger.log(Level.INFO, "Request to create or get chat for item {0}", request.getItemId());
        
        Long userId = getUserIdFromSession(httpRequest);
        if (userId == null) {
            logger.log(Level.WARNING, "Unauthenticated user attempted to create chat");
            return ResponseEntity.status(401).body(NOT_AUTHENTICATED_MSG);
        }

        try {
            ChatResponse response = chatService.createOrGetChat(
                userId, 
                request.getItemId(), 
                request.getInitialMessage()
            );
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            logger.log(Level.WARNING, "Resource not found: {0}", e.getMessage());
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.log(Level.WARNING, "Invalid request: {0}", e.getMessage());
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    /**
     * Get all chats for the authenticated user.
     * GET /api/chats
     */
    @GetMapping
    public ResponseEntity<Object> getUserChats(HttpServletRequest request) {
        logger.log(Level.INFO, "Request to get user chats");
        
        Long userId = getUserIdFromSession(request);
        if (userId == null) {
            logger.log(Level.WARNING, "Unauthenticated user attempted to get chats");
            return ResponseEntity.status(401).body(NOT_AUTHENTICATED_MSG);
        }

        List<ChatResponse> chats = chatService.getUserChats(userId);
        return ResponseEntity.ok(chats);
    }

    @GetMapping("/{chatId}")
    public ResponseEntity<Object> getChat(@PathVariable Long chatId, 
                                          HttpServletRequest request) {
        logger.log(Level.INFO, "Request to get chat {0}", chatId);
        
        Long userId = getUserIdFromSession(request);
        if (userId == null) {
            logger.log(Level.WARNING, "Unauthenticated user attempted to get chat");
            return ResponseEntity.status(401).body(NOT_AUTHENTICATED_MSG);
        }

        try {
            ChatResponse response = chatService.getChat(chatId, userId);
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            logger.log(Level.WARNING, CHAT_NOT_FOUND, e.getMessage());
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (UnauthorizedException e) {
            logger.log(Level.WARNING, "Unauthorized access to chat: {0}", e.getMessage());
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    /**
     * Get all messages in a chat.
     * GET /api/chats/{chatId}/messages
     */
    @GetMapping("/{chatId}/messages")
    public ResponseEntity<Object> getChatMessages(@PathVariable Long chatId, 
                                                   HttpServletRequest request) {
        logger.log(Level.INFO, "Request to get messages for chat {0}", chatId);
        
        Long userId = getUserIdFromSession(request);
        if (userId == null) {
            logger.log(Level.WARNING, "Unauthenticated user attempted to get messages");
            return ResponseEntity.status(401).body(NOT_AUTHENTICATED_MSG);
        }

        try {
            List<MessageResponse> messages = chatService.getChatMessages(chatId, userId);
            return ResponseEntity.ok(messages);
        } catch (NoSuchElementException e) {
            logger.log(Level.WARNING, "Chat not found: {0}", e.getMessage());
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (UnauthorizedException e) {
            logger.log(Level.WARNING, "Unauthorized access to messages: {0}", e.getMessage());
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    /**
     * Send a message in a chat.
     * POST /api/chats/{chatId}/messages
     * Body: { "content": "message text" }
     */
    @PostMapping("/{chatId}/messages")
    public ResponseEntity<Object> sendMessage(@PathVariable Long chatId,
                                               @RequestBody MessageRequest request,
                                               HttpServletRequest httpRequest) {
        logger.log(Level.INFO, "Request to send message in chat {0}", chatId);
        
        Long userId = getUserIdFromSession(httpRequest);
        if (userId == null) {
            logger.log(Level.WARNING, "Unauthenticated user attempted to send message");
            return ResponseEntity.status(401).body(NOT_AUTHENTICATED_MSG);
        }

        try {
            MessageResponse response = chatService.sendMessage(
                chatId, 
                userId, 
                request.getContent()
            );
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            logger.log(Level.WARNING, "Resource not found: {0}", e.getMessage());
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "Invalid message: {0}", e.getMessage());
            return ResponseEntity.status(400).body(e.getMessage());
        } catch (UnauthorizedException e) {
            logger.log(Level.WARNING, "Unauthorized message send: {0}", e.getMessage());
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    /**
     * Mark messages as read in a chat.
     * PUT /api/chats/{chatId}/read
     */
    @PutMapping("/{chatId}/read")
    public ResponseEntity<Object> markMessagesAsRead(@PathVariable Long chatId,
                                                      HttpServletRequest request) {
        logger.log(Level.INFO, "Request to mark messages as read in chat {0}", chatId);
        
        Long userId = getUserIdFromSession(request);
        if (userId == null) {
            logger.log(Level.WARNING, "Unauthenticated user attempted to mark messages as read");
            return ResponseEntity.status(401).body(NOT_AUTHENTICATED_MSG);
        }

        try {
            chatService.markMessagesAsRead(chatId, userId);
            return ResponseEntity.ok().build();
        } catch (NoSuchElementException e) {
            logger.log(Level.WARNING, CHAT_NOT_FOUND, e.getMessage());
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (UnauthorizedException e) {
            logger.log(Level.WARNING, "Unauthorized access: {0}", e.getMessage());
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    /**
     * Extract user ID from HTTP session.
     * Returns null if session doesn't exist or user is not authenticated.
     */
    private Long getUserIdFromSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object userId = session.getAttribute(USER_ID_KEY);
        return (userId instanceof Long l) ? l : null;
    }
}
