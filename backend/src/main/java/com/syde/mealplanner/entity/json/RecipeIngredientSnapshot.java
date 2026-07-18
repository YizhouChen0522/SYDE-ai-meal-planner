package com.syde.mealplanner.entity.json;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RecipeIngredientSnapshot {

    private String name;
    private BigDecimal quantity;
    private String unit;
}
