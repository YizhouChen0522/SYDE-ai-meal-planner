package com.syde.mealplanner.controller;

import com.syde.mealplanner.common.Result;
import com.syde.mealplanner.dto.MealHistoryResponse;
import com.syde.mealplanner.service.MealHistoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/meal-history")
public class MealHistoryController {

    private final MealHistoryService mealHistoryService;

    public MealHistoryController(MealHistoryService mealHistoryService) {
        this.mealHistoryService = mealHistoryService;
    }

    @GetMapping
    public Result<List<MealHistoryResponse>> getAll() {
        return Result.success(mealHistoryService.getCurrentUserHistory());
    }

    @GetMapping("/{id}")
    public Result<MealHistoryResponse> getById(@PathVariable Long id) {
        return Result.success(mealHistoryService.getCurrentUserHistoryById(id));
    }
}
