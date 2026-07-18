package com.syde.mealplanner.entity.json;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class NutritionSnapshot {

    private BigDecimal amount;
    private String unit;
    private BigDecimal dailyValuePercent;
}
