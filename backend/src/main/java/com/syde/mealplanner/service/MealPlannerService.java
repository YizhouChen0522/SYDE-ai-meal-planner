package com.syde.mealplanner.service;

import com.syde.mealplanner.entity.MealPlanDraft;
import com.syde.mealplanner.entity.MealHistory;

public interface MealPlannerService {

    MealPlanDraft generateDraft(String userRequest);

    MealPlanDraft getCurrentDraft();

    void deleteCurrentDraft();

    MealPlanDraft replaceRecipe(Long recipeId);

    MealPlanDraft addRecipe();

    MealPlanDraft deleteRecipe(Long recipeId);

    MealHistory confirmCurrentDraft();
}
