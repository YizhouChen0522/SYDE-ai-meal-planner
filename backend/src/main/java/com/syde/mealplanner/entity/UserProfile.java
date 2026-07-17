package com.syde.mealplanner.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserProfile {

    private Long id;
    private Long userId;
    private List<String> likedFoods;
    private List<String> dislikedFoods;
    private List<String> allergies;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
