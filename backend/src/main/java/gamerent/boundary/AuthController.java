package gamerent.boundary;

import gamerent.data.User;
import gamerent.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final String EMAIL_KEY = "email";
    private static final String USER_ID_KEY = "userId";
    private static final String USER_EMAIL_KEY = "userEmail";
    private static final String USER_ROLE_KEY = "userRole";
    private static final String NAME_KEY = "name";
    private static final String ROLE_KEY = "role";
    private static final String ID_KEY = "id";
    private static final String NOT_AUTHENTICATED_MSG = "Not authenticated";

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<Object> register(@RequestBody User user) {
        if (userService.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already in use");
        }
        user.setRole("USER");
        User saved = userService.registerUser(user);

        // Build response without password
        Map<String, Object> response = new HashMap<>();
        response.put(ID_KEY, saved.getId());
        response.put(EMAIL_KEY, saved.getEmail());
        response.put(ROLE_KEY, saved.getRole());
        response.put(NAME_KEY, saved.getName());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody Map<String, String> loginData, HttpServletRequest request) {
        String email = loginData.get(EMAIL_KEY);
        String password = loginData.get("password");
        Optional<User> userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty() || !userService.checkPassword(userOpt.get(), password)) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
        User user = userOpt.get();
        // Create an authenticated session so subsequent requests are authorized
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + (user.getRole() != null ? user.getRole() : "USER"));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user.getEmail(), null, java.util.List.of(authority));
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);
        // ensure an HTTP session exists and store the security context so Spring Security saves the session cookie
        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        // Store useful info in session for controller access
        session.setAttribute(USER_ID_KEY, user.getId());
        session.setAttribute(USER_EMAIL_KEY, user.getEmail());
        session.setAttribute(USER_ROLE_KEY, user.getRole());
        Map<String, Object> response = new HashMap<>();
        response.put(ID_KEY, user.getId());
        response.put(EMAIL_KEY, user.getEmail());
        response.put(ROLE_KEY, user.getRole());
        response.put(NAME_KEY, user.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<Object> me(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return ResponseEntity.status(401).body(NOT_AUTHENTICATED_MSG);
        Object uid = session.getAttribute(USER_ID_KEY);
        if (uid == null) return ResponseEntity.status(401).body(NOT_AUTHENTICATED_MSG);

        Long userId = null;
        if (uid instanceof Long longValue) userId = longValue;
        else if (uid instanceof Integer intValue) userId = intValue.longValue();

        User user = userService.findByEmail((String) session.getAttribute(USER_EMAIL_KEY)).orElse(null);
        if (user == null) return ResponseEntity.status(401).body(NOT_AUTHENTICATED_MSG);

        Map<String, Object> response = new HashMap<>();
        response.put(ID_KEY, user.getId());
        response.put(EMAIL_KEY, user.getEmail());
        response.put(ROLE_KEY, user.getRole());
        response.put(NAME_KEY, user.getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Object> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }
}
