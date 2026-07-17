package com.syde.mealplanner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InventoryCreateRequest(
        @NotBlank(message = "Food name is required")
        @Size(max = 100, message = "Food name must be at most 100 characters")
        String foodName,

        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive")
        BigDecimal quantity,

        @NotBlank(message = "Unit is required")
        @Size(max = 20, message = "Unit must be at most 20 characters")
        String unit,

        @NotNull(message = "Added date is required")
        LocalDate addedDate,

        @NotNull(message = "Reminder days is required")
        @PositiveOrZero(message = "Reminder days cannot be negative")
        Integer reminderDays) {
}
