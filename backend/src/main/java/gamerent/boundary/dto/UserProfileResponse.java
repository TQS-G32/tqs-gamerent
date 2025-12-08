package gamerent.boundary.dto;

public record UserProfileResponse(
        Long id,
        String name,
        String email,
        Double averageRating,
        int reviewCount,
        int itemsCount
) {}

