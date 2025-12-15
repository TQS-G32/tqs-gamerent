package gamerent.boundary;

import gamerent.boundary.dto.UserProfileResponse;
import gamerent.data.User;
import gamerent.data.UserRepository;
import gamerent.service.UserService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {
    private final UserRepository userRepository;
    private final UserService userService;

    public UserController(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @PostMapping
    public User addUser(@RequestBody User user) {
        return userRepository.save(user);
    }

    @GetMapping("/{id}/profile")
    public UserProfileResponse getProfile(@PathVariable Long id) {
        return userService.getProfile(id);
    }
}
