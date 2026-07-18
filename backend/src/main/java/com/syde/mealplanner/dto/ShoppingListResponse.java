package com.syde.mealplanner.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShoppingListResponse(
        Long id,
        String foodName,
        BigDecimal requiredQuantity,
        BigDecimal availableQuantity,
        BigDecimal quantityToBuy,
        String unit,
        LocalDateTime createTime,
        LocalDateTime updateTime) {
}
