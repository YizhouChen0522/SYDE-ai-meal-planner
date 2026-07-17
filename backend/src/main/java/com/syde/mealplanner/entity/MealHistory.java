package com.syde.mealplanner.entity;

import com.syde.mealplanner.entity.json.RecipeSnapshot;
import com.syde.mealplanner.entity.json.ShoppingListSnapshotItem;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MealHistory {

    private Long id;
    private Long userId;
    private String userRequest;
    private List<RecipeSnapshot> recipes;
    private List<ShoppingListSnapshotItem> shoppingListSnapshot;
    private LocalDateTime confirmedTime;
    private LocalDateTime createTime;
}
