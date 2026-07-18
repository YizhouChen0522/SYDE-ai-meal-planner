package com.syde.mealplanner.provider.impl;

import com.syde.mealplanner.entity.json.NutritionSnapshot;
import com.syde.mealplanner.entity.json.RecipeIngredientSnapshot;
import com.syde.mealplanner.entity.json.RecipeSnapshot;
import com.syde.mealplanner.provider.MealGenerationProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "meal-generation", name = "provider", havingValue = "mock", matchIfMissing = true)
public class MockMealGenerationProvider implements MealGenerationProvider {

    @Override
    public List<RecipeSnapshot> generate(String userRequest) {
        String normalizedRequest = userRequest == null ? "" : userRequest.toLowerCase(Locale.ROOT);
        if (normalizedRequest.contains("vegetarian") || normalizedRequest.contains("veggie")) {
            return List.of(
                    chickpeaPasta(),
                    tofuRiceBowl(),
                    lentilSoup(),
                    mushroomTacos(),
                    quinoaPowerSalad());
        }

        return List.of(
                chickenPasta(),
                salmonRiceBowl(),
                turkeyWrap(),
                beefStirFry(),
                shrimpNoodleSoup());
    }

    @Override
    public RecipeSnapshot generateReplacement(
            String userRequest,
            RecipeSnapshot recipeToReplace,
            List<RecipeSnapshot> currentRecipes) {
        String normalizedRequest = userRequest == null ? "" : userRequest.toLowerCase(Locale.ROOT);
        List<RecipeSnapshot> candidates = normalizedRequest.contains("vegetarian") || normalizedRequest.contains("veggie")
                ? List.of(sweetPotatoBurritoBowl(), mediterraneanBeanSkillet())
                : List.of(mediterraneanChickenBowl(), porkQuinoaSkillet());

        return candidates.stream()
                .filter(candidate -> recipeToReplace == null || !candidate.getId().equals(recipeToReplace.getId()))
                .filter(candidate -> currentRecipes == null || currentRecipes.stream()
                        .map(RecipeSnapshot::getId)
                        .noneMatch(candidate.getId()::equals))
                .findFirst()
                .orElseGet(() -> fallbackReplacement(currentRecipes));
    }

    @Override
    public RecipeSnapshot generateAdditionalRecipe(String userRequest, List<RecipeSnapshot> currentRecipes) {
        String normalizedRequest = userRequest == null ? "" : userRequest.toLowerCase(Locale.ROOT);
        List<RecipeSnapshot> candidates = normalizedRequest.contains("vegetarian") || normalizedRequest.contains("veggie")
                ? List.of(vegetableCurry(), barleyStuffedPeppers())
                : List.of(herbChickenTrayBake(), codPotatoSkillet());

        return candidates.stream()
                .filter(candidate -> currentRecipes == null || currentRecipes.stream()
                        .map(RecipeSnapshot::getId)
                        .noneMatch(candidate.getId()::equals))
                .findFirst()
                .orElseGet(() -> fallbackAdditionalRecipe(currentRecipes));
    }

    private RecipeSnapshot chickenPasta() {
        RecipeSnapshot recipe = new RecipeSnapshot();
        recipe.setId(1001L);
        recipe.setTitle("Lemon Chicken Pasta");
        recipe.setIngredients(List.of(
                ingredient("Chicken breast", "0.45", "kg"),
                ingredient("Pasta", "0.30", "kg"),
                ingredient("Lemon", "1.00", "piece"),
                ingredient("Spinach", "0.15", "kg")));
        recipe.setSteps(List.of(
                "Cook pasta until tender.",
                "Saute chicken with lemon juice.",
                "Combine pasta, chicken, and spinach."));
        recipe.setNutrition(nutrition("protein", "48", "g", "96"));
        return recipe;
    }

    private RecipeSnapshot salmonRiceBowl() {
        RecipeSnapshot recipe = new RecipeSnapshot();
        recipe.setId(1002L);
        recipe.setTitle("Salmon Rice Bowl");
        recipe.setIngredients(List.of(
                ingredient("Salmon fillet", "0.40", "kg"),
                ingredient("Brown rice", "0.35", "kg"),
                ingredient("Broccoli", "0.25", "kg"),
                ingredient("Soy sauce", "0.03", "L")));
        recipe.setSteps(List.of(
                "Bake salmon until flaky.",
                "Steam broccoli.",
                "Serve salmon and broccoli over rice."));
        recipe.setNutrition(nutrition("protein", "42", "g", "84"));
        return recipe;
    }

    private RecipeSnapshot turkeyWrap() {
        RecipeSnapshot recipe = new RecipeSnapshot();
        recipe.setId(1003L);
        recipe.setTitle("Turkey Avocado Wrap");
        recipe.setIngredients(List.of(
                ingredient("Turkey slices", "0.25", "kg"),
                ingredient("Whole wheat tortilla", "3.00", "piece"),
                ingredient("Avocado", "1.00", "piece"),
                ingredient("Lettuce", "0.10", "kg")));
        recipe.setSteps(List.of(
                "Warm tortillas.",
                "Layer turkey, avocado, and lettuce.",
                "Roll tightly and slice."));
        recipe.setNutrition(nutrition("protein", "35", "g", "70"));
        return recipe;
    }

    private RecipeSnapshot chickpeaPasta() {
        RecipeSnapshot recipe = new RecipeSnapshot();
        recipe.setId(2001L);
        recipe.setTitle("Chickpea Tomato Pasta");
        recipe.setIngredients(List.of(
                ingredient("Chickpeas", "0.35", "kg"),
                ingredient("Pasta", "0.30", "kg"),
                ingredient("Tomato sauce", "0.40", "L"),
                ingredient("Spinach", "0.15", "kg")));
        recipe.setSteps(List.of(
                "Cook pasta until tender.",
                "Simmer chickpeas with tomato sauce.",
                "Fold in spinach and combine with pasta."));
        recipe.setNutrition(nutrition("protein", "28", "g", "56"));
        return recipe;
    }

    private RecipeSnapshot tofuRiceBowl() {
        RecipeSnapshot recipe = new RecipeSnapshot();
        recipe.setId(2002L);
        recipe.setTitle("Tofu Rice Bowl");
        recipe.setIngredients(List.of(
                ingredient("Firm tofu", "0.40", "kg"),
                ingredient("Brown rice", "0.35", "kg"),
                ingredient("Broccoli", "0.25", "kg"),
                ingredient("Soy sauce", "0.03", "L")));
        recipe.setSteps(List.of(
                "Pan-sear tofu until crisp.",
                "Steam broccoli.",
                "Serve tofu and broccoli over rice."));
        recipe.setNutrition(nutrition("protein", "32", "g", "64"));
        return recipe;
    }

    private RecipeSnapshot lentilSoup() {
        RecipeSnapshot recipe = new RecipeSnapshot();
        recipe.setId(2003L);
        recipe.setTitle("Lentil Vegetable Soup");
        recipe.setIngredients(List.of(
                ingredient("Lentils", "0.30", "kg"),
                ingredient("Carrot", "0.20", "kg"),
                ingredient("Celery", "0.15", "kg"),
                ingredient("Vegetable broth", "1.00", "L")));
        recipe.setSteps(List.of(
                "Rinse lentils.",
                "Simmer lentils and vegetables in broth.",
                "Cook until lentils are tender."));
        recipe.setNutrition(nutrition("protein", "24", "g", "48"));
        return recipe;
    }

    private RecipeSnapshot beefStirFry() {
        RecipeSnapshot recipe = new RecipeSnapshot();
        recipe.setId(1004L);
        recipe.setTitle("Beef Vegetable Stir Fry");
        recipe.setIngredients(List.of(
                ingredient("Lean beef", "0.40", "kg"),
                ingredient("Bell pepper", "0.20", "kg"),
                ingredient("Snow peas", "0.15", "kg"),
                ingredient("Rice noodles", "0.25", "kg")));
        recipe.setSteps(List.of(
                "Slice beef into thin strips.",
                "Stir-fry beef and vegetables over high heat.",
                "Toss with cooked rice noodles."));
        recipe.setNutrition(nutrition("protein", "44", "g", "88"));
        return recipe;
    }

    private RecipeSnapshot shrimpNoodleSoup() {
        RecipeSnapshot recipe = new RecipeSnapshot();
        recipe.setId(1005L);
        recipe.setTitle("Shrimp Noodle Soup");
        recipe.setIngredients(List.of(
                ingredient("Shrimp", "0.35", "kg"),
                ingredient("Rice noodles", "0.25", "kg"),
                ingredient("Mushrooms", "0.15", "kg"),
                ingredient("Chicken broth", "1.00", "L")));
        recipe.setSteps(List.of(
                "Simmer broth with mushrooms.",
                "Add noodles and cook until tender.",
                "Add shrimp and cook until pink."));
        recipe.setNutrition(nutrition("protein", "38", "g", "76"));
        return recipe;
    }

    private RecipeSnapshot mushroomTacos() {
        RecipeSnapshot recipe = new RecipeSnapshot();
        recipe.setId(2004L);
        recipe.setTitle("Mushroom Black Bean Tacos");
        recipe.setIngredients(List.of(
                ingredient("Mushrooms", "0.30", "kg"),
                ingredient("Black beans", "0.30", "kg"),
                ingredient("Corn tortilla", "4.00", "piece"),
                ingredient("Salsa", "0.20", "kg")));
        recipe.setSteps(List.of(
                "Saute mushrooms until browned.",
                "Warm black beans and tortillas.",
                "Fill tortillas with mushrooms, beans, and salsa."));
        recipe.setNutrition(nutrition("protein", "26", "g", "52"));
        return recipe;
    }

    private RecipeSnapshot quinoaPowerSalad() {
        RecipeSnapshot recipe = new RecipeSnapshot();
        recipe.setId(2005L);
        recipe.setTitle("Quinoa Power Salad");
        recipe.setIngredients(List.of(
                ingredient("Quinoa", "0.30", "kg"),
                ingredient("Cucumber", "0.20", "kg"),
                ingredient("Cherry tomato", "0.20", "kg"),
                ingredient("Feta cheese", "0.10", "kg")));
        recipe.setSteps(List.of(
                "Cook quinoa and let it cool.",
                "Chop vegetables.",
                "Toss quinoa, vegetables, and feta together."));
        recipe.setNutrition(nutrition("protein", "22", "g", "44"));
        return recipe;
    }

    private RecipeSnapshot mediterraneanChickenBowl() {
        RecipeSnapshot recipe = new RecipeSnapshot();
        recipe.setId(3001L);
        recipe.setTitle("Mediterranean Chicken Bowl");
        recipe.setIngredients(List.of(
                ingredient("Chicken breast", "0.40", "kg"),
                ingredient("Couscous", "0.30", "kg"),
                ingredient("Cucumber", "0.20", "kg"),
                ingredient("Greek yogurt", "0.15", "kg")));
        recipe.setSteps(List.of(
                "Grill chicken until cooked through.",
                "Prepare couscous according to package directions.",
                "Serve chicken over couscous with cucumber and yogurt sauce."));
        recipe.setNutrition(nutrition("protein", "46", "g", "92"));
        return recipe;
    }

    private RecipeSnapshot porkQuinoaSkillet() {
        RecipeSnapshot recipe = new RecipeSnapshot();
        recipe.setId(3002L);
        recipe.setTitle("Pork Quinoa Skillet");
        recipe.setIngredients(List.of(
                ingredient("Pork tenderloin", "0.40", "kg"),
                ingredient("Quinoa", "0.30", "kg"),
                ingredient("Zucchini", "0.25", "kg"),
                ingredient("Tomato", "0.20", "kg")));
        recipe.setSteps(List.of(
                "Brown sliced pork in a skillet.",
                "Add vegetables and cook until tender.",
                "Fold in cooked quinoa."));
        recipe.setNutrition(nutrition("protein", "40", "g", "80"));
        return recipe;
    }

    private RecipeSnapshot sweetPotatoBurritoBowl() {
        RecipeSnapshot recipe = new RecipeSnapshot();
        recipe.setId(4001L);
        recipe.setTitle("Sweet Potato Burrito Bowl");
        recipe.setIngredients(List.of(
                ingredient("Sweet potato", "0.45", "kg"),
                ingredient("Black beans", "0.30", "kg"),
                ingredient("Brown rice", "0.30", "kg"),
                ingredient("Corn", "0.20", "kg")));
        recipe.setSteps(List.of(
                "Roast diced sweet potato.",
                "Warm black beans and corn.",
                "Serve over brown rice."));
        recipe.setNutrition(nutrition("protein", "24", "g", "48"));
        return recipe;
    }

    private RecipeSnapshot mediterraneanBeanSkillet() {
        RecipeSnapshot recipe = new RecipeSnapshot();
        recipe.setId(4002L);
        recipe.setTitle("Mediterranean Bean Skillet");
        recipe.setIngredients(List.of(
                ingredient("White beans", "0.35", "kg"),
                ingredient("Zucchini", "0.25", "kg"),
                ingredient("Tomato", "0.30", "kg"),
                ingredient("Feta cheese", "0.10", "kg")));
        recipe.setSteps(List.of(
                "Saute zucchini until tender.",
                "Add beans and tomatoes.",
                "Top with feta before serving."));
        recipe.setNutrition(nutrition("protein", "25", "g", "50"));
        return recipe;
    }

    private RecipeSnapshot herbChickenTrayBake() {
        RecipeSnapshot recipe = new RecipeSnapshot();
        recipe.setId(5001L);
        recipe.setTitle("Herb Chicken Tray Bake");
        recipe.setIngredients(List.of(
                ingredient("Chicken thigh", "0.45", "kg"),
                ingredient("Potato", "0.40", "kg"),
                ingredient("Green beans", "0.20", "kg"),
                ingredient("Olive oil", "0.03", "L")));
        recipe.setSteps(List.of(
                "Arrange chicken and potatoes on a tray.",
                "Roast until the chicken is cooked through.",
                "Add green beans near the end of roasting."));
        recipe.setNutrition(nutrition("protein", "43", "g", "86"));
        return recipe;
    }

    private RecipeSnapshot codPotatoSkillet() {
        RecipeSnapshot recipe = new RecipeSnapshot();
        recipe.setId(5002L);
        recipe.setTitle("Cod Potato Skillet");
        recipe.setIngredients(List.of(
                ingredient("Cod fillet", "0.40", "kg"),
                ingredient("Potato", "0.35", "kg"),
                ingredient("Cherry tomato", "0.20", "kg"),
                ingredient("Parsley", "0.02", "kg")));
        recipe.setSteps(List.of(
                "Pan-cook sliced potatoes until tender.",
                "Add cod and tomatoes.",
                "Cook until cod flakes easily."));
        recipe.setNutrition(nutrition("protein", "39", "g", "78"));
        return recipe;
    }

    private RecipeSnapshot vegetableCurry() {
        RecipeSnapshot recipe = new RecipeSnapshot();
        recipe.setId(6001L);
        recipe.setTitle("Vegetable Coconut Curry");
        recipe.setIngredients(List.of(
                ingredient("Cauliflower", "0.30", "kg"),
                ingredient("Chickpeas", "0.30", "kg"),
                ingredient("Coconut milk", "0.40", "L"),
                ingredient("Brown rice", "0.30", "kg")));
        recipe.setSteps(List.of(
                "Simmer vegetables and chickpeas in coconut milk.",
                "Cook rice separately.",
                "Serve curry over rice."));
        recipe.setNutrition(nutrition("protein", "23", "g", "46"));
        return recipe;
    }

    private RecipeSnapshot barleyStuffedPeppers() {
        RecipeSnapshot recipe = new RecipeSnapshot();
        recipe.setId(6002L);
        recipe.setTitle("Barley Stuffed Peppers");
        recipe.setIngredients(List.of(
                ingredient("Bell pepper", "4.00", "piece"),
                ingredient("Barley", "0.30", "kg"),
                ingredient("White beans", "0.25", "kg"),
                ingredient("Tomato sauce", "0.30", "L")));
        recipe.setSteps(List.of(
                "Cook barley until tender.",
                "Mix barley with beans and tomato sauce.",
                "Stuff peppers and bake until softened."));
        recipe.setNutrition(nutrition("protein", "21", "g", "42"));
        return recipe;
    }

    private RecipeSnapshot fallbackReplacement(List<RecipeSnapshot> currentRecipes) {
        long nextId = currentRecipes == null
                ? 9001L
                : currentRecipes.stream()
                .map(RecipeSnapshot::getId)
                .filter(id -> id != null)
                .max(Long::compareTo)
                .orElse(9000L) + 1L;

        RecipeSnapshot recipe = mediterraneanChickenBowl();
        recipe.setId(nextId);
        return recipe;
    }

    private RecipeSnapshot fallbackAdditionalRecipe(List<RecipeSnapshot> currentRecipes) {
        long nextId = currentRecipes == null
                ? 9101L
                : currentRecipes.stream()
                .map(RecipeSnapshot::getId)
                .filter(id -> id != null)
                .max(Long::compareTo)
                .orElse(9100L) + 1L;

        RecipeSnapshot recipe = herbChickenTrayBake();
        recipe.setId(nextId);
        return recipe;
    }

    private RecipeIngredientSnapshot ingredient(String name, String quantity, String unit) {
        RecipeIngredientSnapshot ingredient = new RecipeIngredientSnapshot();
        ingredient.setName(name);
        ingredient.setQuantity(new BigDecimal(quantity));
        ingredient.setUnit(unit);
        return ingredient;
    }

    private Map<String, NutritionSnapshot> nutrition(String name, String amount, String unit, String dailyValuePercent) {
        NutritionSnapshot nutritionSnapshot = new NutritionSnapshot();
        nutritionSnapshot.setAmount(new BigDecimal(amount));
        nutritionSnapshot.setUnit(unit);
        nutritionSnapshot.setDailyValuePercent(new BigDecimal(dailyValuePercent));

        Map<String, NutritionSnapshot> nutrition = new LinkedHashMap<>();
        nutrition.put(name, nutritionSnapshot);
        return nutrition;
    }
}
