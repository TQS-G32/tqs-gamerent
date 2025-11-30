package gamerent.boundary;

import gamerent.data.User;
import gamerent.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (userService.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already in use");
        }
        user.setRole("USER");
        User saved = userService.registerUser(user);

        // Build response without password
        Map<String, Object> response = new HashMap<>();
        response.put("id", saved.getId());
        response.put("email", saved.getEmail());
        response.put("role", saved.getRole());
        response.put("name", saved.getName());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginData, HttpServletRequest request) {
        String email = loginData.get("email");
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
        session.setAttribute("userId", user.getId());
        session.setAttribute("userEmail", user.getEmail());
        session.setAttribute("userRole", user.getRole());
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("role", user.getRole());
        response.put("name", user.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return ResponseEntity.status(401).body("Not authenticated");
        Object uid = session.getAttribute("userId");
        if (uid == null) return ResponseEntity.status(401).body("Not authenticated");

        Long userId = null;
        if (uid instanceof Long) userId = (Long) uid;
        else if (uid instanceof Integer) userId = ((Integer) uid).longValue();

        User user = userService.findByEmail((String) session.getAttribute("userEmail")).orElse(null);
        if (user == null) return ResponseEntity.status(401).body("Not authenticated");

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("role", user.getRole());
        response.put("name", user.getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }
}
