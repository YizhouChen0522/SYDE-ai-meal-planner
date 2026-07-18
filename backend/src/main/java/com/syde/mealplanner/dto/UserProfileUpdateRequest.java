package com.syde.mealplanner.dto;

import java.util.List;

public record UserProfileUpdateRequest(
        List<String> likedFoods,
        List<String> dislikedFoods,
        List<String> allergies) {
}
