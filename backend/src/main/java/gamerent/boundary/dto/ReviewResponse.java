package gamerent.boundary.dto;

import gamerent.data.ReviewTargetType;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        Long bookingId,
        Long reviewerId,
        String reviewerName,
        ReviewTargetType targetType,
        Long targetId,
        Integer rating,
        String comment,
        LocalDateTime createdAt
) {}

