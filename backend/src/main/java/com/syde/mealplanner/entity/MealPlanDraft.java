package com.syde.mealplanner.entity;

import com.syde.mealplanner.entity.json.RecipeSnapshot;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MealPlanDraft {

    private Long id;
    private Long userId;
    private String userRequest;
    private List<RecipeSnapshot> recipes;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
