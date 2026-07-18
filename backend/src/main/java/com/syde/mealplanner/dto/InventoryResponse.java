package com.syde.mealplanner.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record InventoryResponse(
        Long id,
        String foodName,
        BigDecimal quantity,
        String unit,
        LocalDate addedDate,
        Integer reminderDays,
        LocalDateTime createTime,
        LocalDateTime updateTime) {
}
