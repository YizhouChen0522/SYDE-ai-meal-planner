package com.syde.mealplanner.service;

import com.syde.mealplanner.dto.MealHistoryResponse;

import java.util.List;

public interface MealHistoryService {

    List<MealHistoryResponse> getCurrentUserHistory();

    MealHistoryResponse getCurrentUserHistoryById(Long id);
}
