package com.syde.mealplanner.dto;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresIn,
        UserSummaryResponse user) {
}
