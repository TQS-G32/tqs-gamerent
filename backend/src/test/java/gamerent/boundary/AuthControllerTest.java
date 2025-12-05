package gamerent.boundary;

import com.fasterxml.jackson.databind.ObjectMapper;
import gamerent.data.User;
import gamerent.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockHttpSession;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = gamerent.boundary.AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AuthControllerTest {

    @MockBean
    private UserService userService;
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerLoginMeLogoutFlow() throws Exception {
        // Register
        Map<String, String> reg = Map.of(
                "name", "it-user",
                "email", "it-user@example.com",
                "password", "itpass"
        );

        // Mock userService behavior for register
        when(userService.findByEmail("it-user@example.com")).thenReturn(java.util.Optional.empty());
        when(userService.registerUser(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(123L);
            u.setRole("USER");
            return u;
        });

        mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(reg)))
            .andExpect(status().isOk());

        // Login (create session)
        MockHttpSession session = new MockHttpSession();
        Map<String, String> login = Map.of("email", "it-user@example.com", "password", "itpass");

        User mocked = new User();
        mocked.setId(123L);
        mocked.setEmail("it-user@example.com");
        mocked.setName("it-user");
        mocked.setRole("USER");
        when(userService.findByEmail("it-user@example.com")).thenReturn(java.util.Optional.of(mocked));
        when(userService.checkPassword(mocked, "itpass")).thenReturn(true);

        mockMvc.perform(post("/api/auth/login").session(session)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isOk());

        // GET /me should return OK
        MvcResult meRes = mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andReturn();

        String meBody = meRes.getResponse().getContentAsString();
        Map<String, Object> meMap = objectMapper.readValue(meBody, Map.class);
        assertThat(meMap).containsEntry("email", "it-user@example.com");

        // Logout
        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isOk());

        // After logout, /me should be 401
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized());
    }
}
