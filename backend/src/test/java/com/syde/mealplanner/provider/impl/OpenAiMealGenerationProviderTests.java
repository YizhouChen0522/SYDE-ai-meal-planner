package com.syde.mealplanner.provider.impl;

import com.syde.mealplanner.config.MealGenerationProperties;
import com.syde.mealplanner.entity.json.NutritionSnapshot;
import com.syde.mealplanner.entity.json.RecipeIngredientSnapshot;
import com.syde.mealplanner.entity.json.RecipeSnapshot;
import com.syde.mealplanner.exception.BusinessException;
import com.syde.mealplanner.util.JsonMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenAiMealGenerationProviderTests {

    @Test
    void generateMapsStructuredResponseToFiveRecipesWithUniqueBackendIds() {
        OpenAiMealGenerationProvider provider = providerReturning(recipesResponse(5));

        List<RecipeSnapshot> recipes = provider.generate("high protein dinners");

        assertEquals(5, recipes.size());
        assertCompleteRecipes(recipes);
        assertUniqueIds(recipes);
        assertEquals(List.of(1L, 2L, 3L, 4L, 5L), recipes.stream().map(RecipeSnapshot::getId).toList());
    }

    @Test
    void replacementReturnsOneValidRecipeWithNonConflictingId() {
        List<RecipeSnapshot> currentRecipes = List.of(recipe(10L, "Existing One"), recipe(11L, "Existing Two"));
        OpenAiMealGenerationProvider provider = providerReturning(recipesResponse(1));

        RecipeSnapshot replacement = provider.generateReplacement(
                "balanced dinners",
                currentRecipes.get(0),
                currentRecipes);

        assertCompleteRecipe(replacement);
        assertEquals(12L, replacement.getId());
        assertNotEquals(currentRecipes.get(0).getId(), replacement.getId());
        assertFalse(currentRecipes.stream().map(RecipeSnapshot::getId).anyMatch(replacement.getId()::equals));
    }

    @Test
    void additionalRecipeReturnsOneValidRecipeWithNonConflictingId() {
        List<RecipeSnapshot> currentRecipes = List.of(recipe(20L, "Existing One"), recipe(22L, "Existing Two"));
        OpenAiMealGenerationProvider provider = providerReturning(recipesResponse(1));

        RecipeSnapshot additionalRecipe = provider.generateAdditionalRecipe("balanced dinners", currentRecipes);

        assertCompleteRecipe(additionalRecipe);
        assertEquals(23L, additionalRecipe.getId());
        assertFalse(currentRecipes.stream().map(RecipeSnapshot::getId).anyMatch(additionalRecipe.getId()::equals));
    }

    @Test
    void malformedAiOutputIsRejected() {
        OpenAiMealGenerationProvider provider = providerReturning("""
                {
                  "output": []
                }
                """);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> provider.generate("quick dinners"));

        assertEquals(502, exception.getCode());
        assertEquals("Meal generation provider returned invalid data", exception.getMessage());
    }

    @Test
    void missingRequiredRecipeFieldsAreRejected() {
        OpenAiMealGenerationProvider provider = providerReturning(responseWithGeneratedJson("""
                {
                  "recipes": [
                    {
                      "id": 1,
                      "title": "",
                      "ingredients": [
                        {
                          "name": "Chicken breast",
                          "quantity": 0.4,
                          "unit": "kg"
                        }
                      ],
                      "steps": ["Cook chicken."],
                      "nutrition": {
                        "protein": {
                          "amount": 35,
                          "unit": "g",
                          "dailyValuePercent": 70
                        }
                      }
                    }
                  ]
                }
                """));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> provider.generateAdditionalRecipe("quick dinners", List.of()));

        assertEquals(502, exception.getCode());
        assertEquals("Meal generation provider returned invalid data", exception.getMessage());
    }

    @Test
    void providerErrorIsConvertedToSafeBusinessException() {
        OpenAiMealGenerationProvider provider = new OpenAiMealGenerationProvider(
                properties(),
                request -> {
                    throw new RuntimeException("connection failed with sensitive details");
                });

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> provider.generate("quick dinners"));

        assertEquals(503, exception.getCode());
        assertEquals("Meal generation failed", exception.getMessage());
    }

    private OpenAiMealGenerationProvider providerReturning(String responseBody) {
        return new OpenAiMealGenerationProvider(properties(), request -> responseBody);
    }

    private MealGenerationProperties properties() {
        MealGenerationProperties properties = new MealGenerationProperties();
        properties.getOpenai().setApiKey("test-api-key");
        properties.getOpenai().setModel("test-model");
        return properties;
    }

    private String recipesResponse(int count) {
        StringBuilder recipes = new StringBuilder();
        for (int index = 1; index <= count; index++) {
            if (index > 1) {
                recipes.append(",");
            }
            recipes.append("""
                    {
                      "id": %d,
                      "title": "AI Test Recipe %d",
                      "ingredients": [
                        {
                          "name": "Chicken breast",
                          "quantity": 0.4,
                          "unit": "kg"
                        },
                        {
                          "name": "Brown rice",
                          "quantity": 0.3,
                          "unit": "kg"
                        }
                      ],
                      "steps": [
                        "Cook the rice.",
                        "Cook the chicken.",
                        "Serve together."
                      ],
                      "nutrition": {
                        "calories": {
                          "amount": 520,
                          "unit": "kcal",
                          "dailyValuePercent": 26
                        },
                        "protein": {
                          "amount": 42,
                          "unit": "g",
                          "dailyValuePercent": 84
                        },
                        "carbohydrates": {
                          "amount": 58,
                          "unit": "g",
                          "dailyValuePercent": 21
                        },
                        "fat": {
                          "amount": 14,
                          "unit": "g",
                          "dailyValuePercent": 18
                        }
                      }
                    }
                    """.formatted(100 + index, index));
        }
        return responseWithGeneratedJson("""
                {
                  "recipes": [
                    %s
                  ]
                }
                """.formatted(recipes));
    }

    private String responseWithGeneratedJson(String generatedJson) {
        try {
            return JsonMapper.getObjectMapper().writeValueAsString(Map.of(
                    "output", List.of(Map.of(
                            "content", List.of(Map.of(
                                    "type", "output_text",
                                    "text", generatedJson))))));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not build test response", exception);
        }
    }

    private RecipeSnapshot recipe(Long id, String title) {
        RecipeSnapshot recipe = new RecipeSnapshot();
        recipe.setId(id);
        recipe.setTitle(title);
        recipe.setIngredients(List.of(ingredient()));
        recipe.setSteps(List.of("Prepare ingredients.", "Cook until done."));
        recipe.setNutrition(Map.of("protein", nutrition()));
        return recipe;
    }

    private RecipeIngredientSnapshot ingredient() {
        RecipeIngredientSnapshot ingredient = new RecipeIngredientSnapshot();
        ingredient.setName("Chicken breast");
        ingredient.setQuantity(new BigDecimal("0.4"));
        ingredient.setUnit("kg");
        return ingredient;
    }

    private NutritionSnapshot nutrition() {
        NutritionSnapshot nutrition = new NutritionSnapshot();
        nutrition.setAmount(new BigDecimal("42"));
        nutrition.setUnit("g");
        nutrition.setDailyValuePercent(new BigDecimal("84"));
        return nutrition;
    }

    private void assertCompleteRecipes(List<RecipeSnapshot> recipes) {
        recipes.forEach(this::assertCompleteRecipe);
    }

    private void assertCompleteRecipe(RecipeSnapshot recipe) {
        assertNotNull(recipe);
        assertNotNull(recipe.getId());
        assertNotNull(recipe.getTitle());
        assertFalse(recipe.getTitle().isBlank());
        assertNotNull(recipe.getIngredients());
        assertFalse(recipe.getIngredients().isEmpty());
        assertNotNull(recipe.getSteps());
        assertFalse(recipe.getSteps().isEmpty());
        assertNotNull(recipe.getNutrition());
        assertFalse(recipe.getNutrition().isEmpty());
    }

    private void assertUniqueIds(List<RecipeSnapshot> recipes) {
        Set<Long> ids = new HashSet<>();
        recipes.forEach(recipe -> ids.add(recipe.getId()));
        assertEquals(recipes.size(), ids.size());
    }
}
