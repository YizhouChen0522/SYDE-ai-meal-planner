package com.syde.mealplanner.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ShoppingList {

    private Long id;
    private Long userId;
    private String foodName;
    private BigDecimal requiredQuantity;
    private BigDecimal availableQuantity;
    private BigDecimal quantityToBuy;
    private String unit;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
