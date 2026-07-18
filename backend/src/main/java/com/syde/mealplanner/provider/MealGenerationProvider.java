package com.syde.mealplanner.provider;

import com.syde.mealplanner.entity.json.RecipeSnapshot;

import java.util.List;

public interface MealGenerationProvider {

    List<RecipeSnapshot> generate(String userRequest);

    RecipeSnapshot generateReplacement(
            String userRequest,
            RecipeSnapshot recipeToReplace,
            List<RecipeSnapshot> currentRecipes);

    RecipeSnapshot generateAdditionalRecipe(
            String userRequest,
            List<RecipeSnapshot> currentRecipes);
}
