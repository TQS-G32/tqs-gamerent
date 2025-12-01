package gamerent.service;

import gamerent.data.User;
import gamerent.data.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void registerUser_encodesPasswordAndSaves() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword("plainpass");

        when(passwordEncoder.encode("plainpass")).thenReturn("encodedpass");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(42L);
            return u;
        });

        User saved = userService.registerUser(user);

        assertNotNull(saved);
        assertEquals(42L, saved.getId());
        assertEquals("encodedpass", saved.getPassword());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(passwordEncoder).encode("plainpass");
        verify(userRepository).save(captor.capture());
        User passedToSave = captor.getValue();
        assertEquals("encodedpass", passedToSave.getPassword());
        assertEquals("test@example.com", passedToSave.getEmail());
    }

    @Test
    void checkPassword_delegatesToEncoder() {
        User user = new User();
        user.setPassword("encodedpass");

        when(passwordEncoder.matches("raw", "encodedpass")).thenReturn(true);

        boolean ok = userService.checkPassword(user, "raw");

        assertTrue(ok);
        verify(passwordEncoder).matches("raw", "encodedpass");
    }
}
