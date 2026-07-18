package com.syde.mealplanner.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InventoryUpdateRequest(
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive")
        BigDecimal quantity,

        @NotNull(message = "Added date is required")
        LocalDate addedDate,

        @NotNull(message = "Reminder days is required")
        @PositiveOrZero(message = "Reminder days cannot be negative")
        Integer reminderDays) {
}
