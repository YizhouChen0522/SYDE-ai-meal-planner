package com.syde.mealplanner.service.impl;

import com.syde.mealplanner.dto.MealHistoryResponse;
import com.syde.mealplanner.entity.MealHistory;
import com.syde.mealplanner.exception.BusinessException;
import com.syde.mealplanner.mapper.MealHistoryMapper;
import com.syde.mealplanner.security.AuthenticatedUser;
import com.syde.mealplanner.service.MealHistoryService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MealHistoryServiceImpl implements MealHistoryService {

    private final MealHistoryMapper mealHistoryMapper;

    public MealHistoryServiceImpl(MealHistoryMapper mealHistoryMapper) {
        this.mealHistoryMapper = mealHistoryMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MealHistoryResponse> getCurrentUserHistory() {
        Long userId = getCurrentUserId();
        return mealHistoryMapper.selectAllByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MealHistoryResponse getCurrentUserHistoryById(Long id) {
        MealHistory mealHistory = mealHistoryMapper.selectByIdAndUserId(id, getCurrentUserId());
        if (mealHistory == null) {
            throw new BusinessException(404, "Meal history not found");
        }
        return toResponse(mealHistory);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)) {
            throw new BusinessException(401, "Authentication is required");
        }
        return authenticatedUser.id();
    }

    private MealHistoryResponse toResponse(MealHistory mealHistory) {
        return new MealHistoryResponse(
                mealHistory.getId(),
                mealHistory.getUserRequest(),
                mealHistory.getRecipes(),
                mealHistory.getShoppingListSnapshot(),
                mealHistory.getConfirmedTime(),
                mealHistory.getCreateTime());
    }
}
