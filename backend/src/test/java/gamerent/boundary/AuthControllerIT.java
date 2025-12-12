package gamerent.boundary;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import app.getxray.xray.junit.customjunitxml.annotations.XrayTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import gamerent.data.User;
import gamerent.data.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@Requirement("Auth")
class AuthControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Clean up test user if exists
        userRepository.findByEmail("authtest@example.com").ifPresent(userRepository::delete);
    }

    @Test
    @XrayTest(key = "AUTH-1")
    @Tag("integration")
    void registerUser_ShouldCreateNewUser() throws Exception {
        Map<String, String> registerRequest = Map.of(
                "name", "Auth Test User",
                "email", "authtest@example.com",
                "password", "password123"
        );

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("authtest@example.com"))
                .andExpect(jsonPath("$.name").value("Auth Test User"))
                .andExpect(jsonPath("$.role").value("USER"));

        // Verify user exists in database
        assertThat(userRepository.findByEmail("authtest@example.com")).isPresent();
    }

    @Test
    @XrayTest(key = "AUTH-2")
    @Tag("integration")
    void registerUser_WithExistingEmail_ShouldReturnBadRequest() throws Exception {
        // Create user first
        User existingUser = new User();
        existingUser.setName("Existing User");
        existingUser.setEmail("existing@example.com");
        existingUser.setPassword("password");
        existingUser.setRole("USER");
        userRepository.save(existingUser);

        Map<String, String> registerRequest = Map.of(
                "name", "New User",
                "email", "existing@example.com",
                "password", "password123"
        );

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());

        // Cleanup
        userRepository.delete(existingUser);
    }

    @Test
    @XrayTest(key = "AUTH-3")
    @Tag("integration")
    void loginUser_WithValidCredentials_ShouldCreateSession() throws Exception {
        // Create user first
        User user = new User();
        user.setName("Login Test User");
        user.setEmail("logintest@example.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole("USER");
        user = userRepository.save(user);

        MockHttpSession session = new MockHttpSession();
        Map<String, String> loginRequest = Map.of(
                "email", "logintest@example.com",
                "password", "password123"
        );

        mockMvc.perform(post("/api/auth/login")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("logintest@example.com"));

        // Verify session was created
        assertThat(session.getAttribute("userId")).isNotNull();

        // Cleanup
        userRepository.delete(user);
    }

    @Test
    @XrayTest(key = "AUTH-4")
    @Tag("integration")
    void loginUser_WithInvalidCredentials_ShouldReturnUnauthorized() throws Exception {
        Map<String, String> loginRequest = Map.of(
                "email", "nonexistent@example.com",
                "password", "wrongpassword"
        );

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @XrayTest(key = "AUTH-5")
    @Tag("integration")
    void getMe_WithValidSession_ShouldReturnUserInfo() throws Exception {
        // Create user and login
        User user = new User();
        user.setName("Me Test User");
        user.setEmail("metest@example.com");
        user.setPassword("password123");
        user.setRole("USER");
        user = userRepository.save(user);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", user.getId());
        session.setAttribute("userEmail", user.getEmail());
        session.setAttribute("userRole", user.getRole());

        mockMvc.perform(get("/api/auth/me")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("metest@example.com"))
                .andExpect(jsonPath("$.id").value(user.getId()));

        // Cleanup
        userRepository.delete(user);
    }

    @Test
    @XrayTest(key = "AUTH-6")
    @Tag("integration")
    void getMe_WithoutSession_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @XrayTest(key = "AUTH-7")
    @Tag("integration")
    void logout_ShouldInvalidateSession() throws Exception {
        // Create user and login
        User user = new User();
        user.setName("Logout Test User");
        user.setEmail("logouttest@example.com");
        user.setPassword("password123");
        user.setRole("USER");
        user = userRepository.save(user);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", user.getId());
        session.setAttribute("userEmail", user.getEmail());

        mockMvc.perform(post("/api/auth/logout")
                .session(session))
                .andExpect(status().isOk());

        // Verify session is invalidated - /me should return 401
        mockMvc.perform(get("/api/auth/me")
                .session(session))
                .andExpect(status().isUnauthorized());

        // Cleanup
        userRepository.delete(user);
    }

    @Test
    @XrayTest(key = "AUTH-8")
    @Tag("integration")
    void fullAuthFlow_RegisterLoginMeLogout_ShouldWork() throws Exception {
        MockHttpSession session = new MockHttpSession();

        // 1. Register
        Map<String, String> registerRequest = Map.of(
                "name", "Flow Test User",
                "email", "flowtest@example.com",
                "password", "password123"
        );

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // 2. Login
        Map<String, String> loginRequest = Map.of(
                "email", "flowtest@example.com",
                "password", "password123"
        );

        mockMvc.perform(post("/api/auth/login")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());

        // 3. Get /me
        mockMvc.perform(get("/api/auth/me")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("flowtest@example.com"));

        // 4. Logout
        mockMvc.perform(post("/api/auth/logout")
                .session(session))
                .andExpect(status().isOk());

        // 5. Verify /me returns 401 after logout
        mockMvc.perform(get("/api/auth/me")
                .session(session))
                .andExpect(status().isUnauthorized());

        // Cleanup
        userRepository.findByEmail("flowtest@example.com").ifPresent(userRepository::delete);
    }
}
