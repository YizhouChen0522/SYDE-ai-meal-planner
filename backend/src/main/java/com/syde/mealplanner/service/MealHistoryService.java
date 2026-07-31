package com.syde.mealplanner.service;

import com.syde.mealplanner.dto.MealHistoryResponse;
import com.syde.mealplanner.dto.PageResponse;

import java.util.List;

public interface MealHistoryService {

    List<MealHistoryResponse> getCurrentUserHistory();

    PageResponse<MealHistoryResponse> getCurrentUserHistoryPage(int page, int size);

    MealHistoryResponse getCurrentUserHistoryById(Long id);
}
