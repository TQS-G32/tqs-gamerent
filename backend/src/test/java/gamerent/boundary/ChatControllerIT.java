package gamerent.boundary;

import com.fasterxml.jackson.databind.ObjectMapper;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import gamerent.boundary.dto.ChatRequest;
import gamerent.boundary.dto.MessageRequest;
import gamerent.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ChatController.
 * Tests REST API endpoints with real beans and database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@Requirement("US8")
class ChatControllerIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private User renter;
    private User owner;
    private Item item;

    @BeforeEach
    void setUp() {
        messageRepository.deleteAll();
        chatRepository.deleteAll();
        itemRepository.deleteAll();
        userRepository.deleteAll();

        // Create test users
        renter = new User();
        renter.setEmail("renter@test.com");
        renter.setName("Renter User");
        renter.setPassword("password");
        renter.setRole("RENTER");
        renter = userRepository.save(renter);

        owner = new User();
        owner.setEmail("owner@test.com");
        owner.setName("Owner User");
        owner.setPassword("password");
        owner.setRole("OWNER");
        owner = userRepository.save(owner);

        // Create test item
        item = new Item();
        item.setName("Test Game");
        item.setOwner(owner);
        item.setCategory("Console");
        item.setPricePerDay(10.0);
        item.setAvailable(true);
        item = itemRepository.save(item);
    }

    @Test
    void createOrGetChat_whenAuthenticated_returnsChat() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", renter.getId());

        ChatRequest request = new ChatRequest(item.getId(), "Hello!");

        mockMvc.perform(post("/api/chats")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemId").value(item.getId()))
                .andExpect(jsonPath("$.itemName").value("Test Game"))
                .andExpect(jsonPath("$.renterName").value("Renter User"))
                .andExpect(jsonPath("$.ownerName").value("Owner User"));
    }

    @Test
    void createOrGetChat_whenNotAuthenticated_returns401() throws Exception {
        ChatRequest request = new ChatRequest(item.getId(), "Hello!");

        mockMvc.perform(post("/api/chats")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createOrGetChat_whenItemNotFound_returns404() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", renter.getId());

        ChatRequest request = new ChatRequest(999L, null);

        mockMvc.perform(post("/api/chats")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createOrGetChat_whenUserIsOwner_returns400() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", owner.getId());

        ChatRequest request = new ChatRequest(item.getId(), null);

        mockMvc.perform(post("/api/chats")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ========================
    // GET /api/chats
    // ========================

    @Test
    void getUserChats_whenAuthenticated_returnsChats() throws Exception {
        // Create chats with messages
        Chat chat1 = new Chat();
        chat1.setRenter(renter);
        chat1.setOwner(owner);
        chat1.setItem(item);
        chat1 = chatRepository.save(chat1);

        Message msg1 = new Message();
        msg1.setChat(chat1);
        msg1.setSender(renter);
        msg1.setContent("Hello");
        msg1.setSentAt(java.time.LocalDateTime.now());
        messageRepository.save(msg1);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", renter.getId());

        mockMvc.perform(get("/api/chats")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].itemName").value("Test Game"));
    }

    @Test
    void getUserChats_whenNotAuthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/chats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUserChats_whenNoChats_returnsEmptyArray() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", renter.getId());

        mockMvc.perform(get("/api/chats")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getChat_whenAuthenticated_returnsChat() throws Exception {
        Chat chat = new Chat();
        chat.setRenter(renter);
        chat.setOwner(owner);
        chat.setItem(item);
        chat = chatRepository.save(chat);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", renter.getId());

        mockMvc.perform(get("/api/chats/" + chat.getId())
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemName").value("Test Game"));
    }

    @Test
    void getChat_whenNotAuthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/chats/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getChat_whenUnauthorized_returns403() throws Exception {
        // Create chat between renter and owner
        Chat chat = new Chat();
        chat.setRenter(renter);
        chat.setOwner(owner);
        chat.setItem(item);
        chat = chatRepository.save(chat);

        // Create another user not part of the chat
        User otherUser = new User();
        otherUser.setEmail("other@test.com");
        otherUser.setName("Other User");
        otherUser.setPassword("password");
        otherUser.setRole("RENTER");
        otherUser = userRepository.save(otherUser);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", otherUser.getId());

        mockMvc.perform(get("/api/chats/" + chat.getId())
                .session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void getChat_whenNotFound_returns404() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", renter.getId());

        mockMvc.perform(get("/api/chats/999")
                .session(session))
                .andExpect(status().isNotFound());
    }

    // ========================
    // GET /api/chats/{chatId}/messages
    // ========================

    @Test
    void getChatMessages_whenAuthenticated_returnsMessages() throws Exception {
        Chat chat = new Chat();
        chat.setRenter(renter);
        chat.setOwner(owner);
        chat.setItem(item);
        chat = chatRepository.save(chat);

        Message msg1 = new Message();
        msg1.setChat(chat);
        msg1.setSender(renter);
        msg1.setContent("Hello");
        msg1.setSentAt(java.time.LocalDateTime.now());
        messageRepository.save(msg1);

        Message msg2 = new Message();
        msg2.setChat(chat);
        msg2.setSender(owner);
        msg2.setContent("Hi there");
        msg2.setSentAt(java.time.LocalDateTime.now());
        messageRepository.save(msg2);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", renter.getId());

        mockMvc.perform(get("/api/chats/" + chat.getId() + "/messages")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].content").value("Hello"))
                .andExpect(jsonPath("$[1].content").value("Hi there"));
    }

    @Test
    void getChatMessages_whenNotAuthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/chats/1/messages"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getChatMessages_whenUnauthorized_returns403() throws Exception {
        Chat chat = new Chat();
        chat.setRenter(renter);
        chat.setOwner(owner);
        chat.setItem(item);
        chat = chatRepository.save(chat);

        User otherUser = new User();
        otherUser.setEmail("other2@test.com");
        otherUser.setName("Other User 2");
        otherUser.setPassword("password");
        otherUser.setRole("RENTER");
        otherUser = userRepository.save(otherUser);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", otherUser.getId());

        mockMvc.perform(get("/api/chats/" + chat.getId() + "/messages")
                .session(session))
                .andExpect(status().isForbidden());
    }

    // ========================
    // POST /api/chats/{chatId}/messages
    // ========================

    @Test
    void sendMessage_whenAuthenticated_returnsMessage() throws Exception {
        Chat chat = new Chat();
        chat.setRenter(renter);
        chat.setOwner(owner);
        chat.setItem(item);
        chat = chatRepository.save(chat);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", renter.getId());

        MessageRequest request = new MessageRequest("Hello!");

        mockMvc.perform(post("/api/chats/" + chat.getId() + "/messages")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Hello!"));
    }

    @Test
    void sendMessage_whenNotAuthenticated_returns401() throws Exception {
        MessageRequest request = new MessageRequest("Hello!");

        mockMvc.perform(post("/api/chats/1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sendMessage_whenEmptyContent_returns400() throws Exception {
        Chat chat = new Chat();
        chat.setRenter(renter);
        chat.setOwner(owner);
        chat.setItem(item);
        chat = chatRepository.save(chat);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", renter.getId());

        MessageRequest request = new MessageRequest("");

        mockMvc.perform(post("/api/chats/" + chat.getId() + "/messages")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sendMessage_whenUnauthorized_returns403() throws Exception {
        Chat chat = new Chat();
        chat.setRenter(renter);
        chat.setOwner(owner);
        chat.setItem(item);
        chat = chatRepository.save(chat);

        User otherUser = new User();
        otherUser.setEmail("other3@test.com");
        otherUser.setName("Other User 3");
        otherUser.setPassword("password");
        otherUser.setRole("RENTER");
        otherUser = userRepository.save(otherUser);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", otherUser.getId());

        MessageRequest request = new MessageRequest("Hello!");

        mockMvc.perform(post("/api/chats/" + chat.getId() + "/messages")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ========================
    // PUT /api/chats/{chatId}/read
    // ========================

    @Test
    void markMessagesAsRead_whenAuthenticated_returns200() throws Exception {
        Chat chat = new Chat();
        chat.setRenter(renter);
        chat.setOwner(owner);
        chat.setItem(item);
        chat = chatRepository.save(chat);

        Message msg = new Message();
        msg.setChat(chat);
        msg.setSender(owner);
        msg.setContent("Hello");
        msg.setSentAt(java.time.LocalDateTime.now());
        messageRepository.save(msg);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", renter.getId());

        mockMvc.perform(put("/api/chats/" + chat.getId() + "/read")
                .session(session))
                .andExpect(status().isOk());
    }

    @Test
    void markMessagesAsRead_whenNotAuthenticated_returns401() throws Exception {
        mockMvc.perform(put("/api/chats/1/read"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void markMessagesAsRead_whenUnauthorized_returns403() throws Exception {
        Chat chat = new Chat();
        chat.setRenter(renter);
        chat.setOwner(owner);
        chat.setItem(item);
        chat = chatRepository.save(chat);

        User otherUser = new User();
        otherUser.setEmail("other4@test.com");
        otherUser.setName("Other User 4");
        otherUser.setPassword("password");
        otherUser.setRole("RENTER");
        otherUser = userRepository.save(otherUser);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", otherUser.getId());

        mockMvc.perform(put("/api/chats/" + chat.getId() + "/read")
                .session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void markMessagesAsRead_whenChatNotFound_returns404() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", renter.getId());

        mockMvc.perform(put("/api/chats/999/read")
                .session(session))
                .andExpect(status().isNotFound());
    }

}
