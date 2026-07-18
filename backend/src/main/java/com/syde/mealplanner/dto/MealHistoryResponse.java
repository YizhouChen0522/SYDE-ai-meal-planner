package com.syde.mealplanner.dto;

import com.syde.mealplanner.entity.json.RecipeSnapshot;
import com.syde.mealplanner.entity.json.ShoppingListSnapshotItem;

import java.time.LocalDateTime;
import java.util.List;

public record MealHistoryResponse(
        Long id,
        String userRequest,
        List<RecipeSnapshot> recipes,
        List<ShoppingListSnapshotItem> shoppingListSnapshot,
        LocalDateTime confirmedTime,
        LocalDateTime createTime) {
}
