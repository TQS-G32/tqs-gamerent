package gamerent.boundary.dto;

import java.util.List;
import gamerent.data.Item;

public record UserProfileResponse(
        Long id,
        String name,
        String email,
        Double averageRating,
        int reviewCount,
        int itemsCount,
        List<Item> items,
        List<?> reviews
) {}

