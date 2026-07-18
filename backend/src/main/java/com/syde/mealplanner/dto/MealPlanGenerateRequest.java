package com.syde.mealplanner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MealPlanGenerateRequest(
        @NotBlank(message = "User request is required")
        @Size(max = 500, message = "User request must be at most 500 characters")
        String userRequest) {
}
