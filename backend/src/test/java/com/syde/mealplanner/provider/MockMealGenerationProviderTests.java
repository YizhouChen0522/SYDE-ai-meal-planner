package com.syde.mealplanner.provider;

import com.syde.mealplanner.entity.json.RecipeSnapshot;
import com.syde.mealplanner.provider.impl.MockMealGenerationProvider;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MockMealGenerationProviderTests {

    private final MealGenerationProvider provider = new MockMealGenerationProvider();

    @Test
    void generateReturnsFiveCompleteRecipesWithUniqueIds() {
        assertRecipes(provider.generate("high protein dinners"));
        assertRecipes(provider.generate("vegetarian dinners"));
    }

    @Test
    void generateReplacementReturnsOneCompleteRecipeWithUniqueId() {
        List<RecipeSnapshot> currentRecipes = provider.generate("high protein dinners");
        RecipeSnapshot recipeToReplace = currentRecipes.get(0);

        RecipeSnapshot replacement = provider.generateReplacement(
                "high protein dinners",
                recipeToReplace,
                currentRecipes);

        assertNotNull(replacement);
        assertNotNull(replacement.getId());
        assertFalse(replacement.getId().equals(recipeToReplace.getId()));
        assertFalse(currentRecipes.stream()
                .map(RecipeSnapshot::getId)
                .anyMatch(replacement.getId()::equals));
        assertFalse(replacement.getTitle().isBlank());
        assertNotNull(replacement.getIngredients());
        assertFalse(replacement.getIngredients().isEmpty());
        assertNotNull(replacement.getSteps());
        assertFalse(replacement.getSteps().isEmpty());
        assertNotNull(replacement.getNutrition());
        assertFalse(replacement.getNutrition().isEmpty());
    }

    @Test
    void generateAdditionalRecipeReturnsOneCompleteRecipeWithUniqueId() {
        List<RecipeSnapshot> currentRecipes = provider.generate("high protein dinners");

        RecipeSnapshot additionalRecipe = provider.generateAdditionalRecipe(
                "high protein dinners",
                currentRecipes);

        assertNotNull(additionalRecipe);
        assertNotNull(additionalRecipe.getId());
        assertFalse(currentRecipes.stream()
                .map(RecipeSnapshot::getId)
                .anyMatch(additionalRecipe.getId()::equals));
        assertFalse(additionalRecipe.getTitle().isBlank());
        assertNotNull(additionalRecipe.getIngredients());
        assertFalse(additionalRecipe.getIngredients().isEmpty());
        assertNotNull(additionalRecipe.getSteps());
        assertFalse(additionalRecipe.getSteps().isEmpty());
        assertNotNull(additionalRecipe.getNutrition());
        assertFalse(additionalRecipe.getNutrition().isEmpty());
    }

    private void assertRecipes(List<RecipeSnapshot> recipes) {
        assertEquals(5, recipes.size());

        Set<Long> recipeIds = new HashSet<>();
        for (RecipeSnapshot recipe : recipes) {
            assertNotNull(recipe.getId());
            recipeIds.add(recipe.getId());
            assertNotNull(recipe.getTitle());
            assertFalse(recipe.getTitle().isBlank());
            assertNotNull(recipe.getIngredients());
            assertFalse(recipe.getIngredients().isEmpty());
            assertNotNull(recipe.getSteps());
            assertFalse(recipe.getSteps().isEmpty());
            assertNotNull(recipe.getNutrition());
            assertFalse(recipe.getNutrition().isEmpty());
        }

        assertEquals(5, recipeIds.size());
    }
}
