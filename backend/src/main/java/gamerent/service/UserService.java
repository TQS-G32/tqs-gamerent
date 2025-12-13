package gamerent.service;

import gamerent.boundary.dto.UserProfileResponse;
import gamerent.data.ReviewRepository;
import gamerent.data.ReviewTargetType;
import gamerent.data.User;
import gamerent.data.UserRepository;
import gamerent.data.ItemRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.DoubleSummaryStatistics;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ReviewRepository reviewRepository;
    private final ItemRepository itemRepository;

    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, ReviewRepository reviewRepository, ItemRepository itemRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.reviewRepository = reviewRepository;
        this.itemRepository = itemRepository;
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User registerUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public boolean checkPassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "User not found"));
        var reviews = reviewRepository.findByTargetTypeAndTargetId(ReviewTargetType.USER, userId);

        DoubleSummaryStatistics stats = reviews.stream()
                .map(Review -> Review.getRating() != null ? Review.getRating() : 0)
                .mapToDouble(Integer::doubleValue)
                .summaryStatistics();

        double average = stats.getCount() > 0 ? Math.round((stats.getAverage()) * 10.0) / 10.0 : 0.0;
        java.util.List<gamerent.data.Item> items = itemRepository.findByOwnerId(userId);
        int itemsCount = items.size();

        return new UserProfileResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            average,
            (int) stats.getCount(),
            itemsCount,
            items,
            reviews
        );
    }
}
