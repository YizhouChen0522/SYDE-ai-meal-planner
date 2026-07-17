package com.syde.mealplanner.entity.json;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class RecipeSnapshot {

    private Long id;
    private String title;
    private List<RecipeIngredientSnapshot> ingredients;
    private List<String> steps;
    private Map<String, NutritionSnapshot> nutrition;
}
