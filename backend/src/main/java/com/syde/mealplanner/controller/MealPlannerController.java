package com.syde.mealplanner.controller;

import com.syde.mealplanner.common.Result;
import com.syde.mealplanner.dto.MealPlanGenerateRequest;
import com.syde.mealplanner.entity.MealHistory;
import com.syde.mealplanner.entity.MealPlanDraft;
import com.syde.mealplanner.service.MealPlannerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meal-plans")
public class MealPlannerController {

    private final MealPlannerService mealPlannerService;

    public MealPlannerController(MealPlannerService mealPlannerService) {
        this.mealPlannerService = mealPlannerService;
    }

    @PostMapping("/generate")
    public Result<MealPlanDraft> generate(@Valid @RequestBody MealPlanGenerateRequest request) {
        return Result.success(mealPlannerService.generateDraft(request.userRequest()));
    }

    @GetMapping("/current")
    public Result<MealPlanDraft> getCurrent() {
        return Result.success(mealPlannerService.getCurrentDraft());
    }

    @DeleteMapping("/current")
    public Result<Void> deleteCurrent() {
        mealPlannerService.deleteCurrentDraft();
        return Result.success();
    }

    @PutMapping("/current/recipes/{recipeId}/replace")
    public Result<MealPlanDraft> replaceRecipe(@PathVariable Long recipeId) {
        return Result.success(mealPlannerService.replaceRecipe(recipeId));
    }

    @PostMapping("/current/recipes")
    public Result<MealPlanDraft> addRecipe() {
        return Result.success(mealPlannerService.addRecipe());
    }

    @DeleteMapping("/current/recipes/{recipeId}")
    public Result<MealPlanDraft> deleteRecipe(@PathVariable Long recipeId) {
        return Result.success(mealPlannerService.deleteRecipe(recipeId));
    }

    @PostMapping("/current/confirm")
    public Result<MealHistory> confirmCurrentDraft() {
        return Result.success(mealPlannerService.confirmCurrentDraft());
    }
}
