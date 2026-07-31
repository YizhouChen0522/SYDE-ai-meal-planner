package com.syde.mealplanner.service.impl;

import com.syde.mealplanner.dto.MealHistoryResponse;
import com.syde.mealplanner.dto.PageResponse;
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

    private static final int MAX_PAGE_SIZE = 50;

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
    public PageResponse<MealHistoryResponse> getCurrentUserHistoryPage(int page, int size) {
        if (page < 1) {
            throw new BusinessException(400, "Page must be at least 1");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(400, "Page size must be between 1 and 50");
        }

        Long userId = getCurrentUserId();
        long total = mealHistoryMapper.countByUserId(userId);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
        int offset = (page - 1) * size;
        List<MealHistoryResponse> records = mealHistoryMapper.selectPageByUserId(userId, size, offset)
                .stream()
                .map(this::toResponse)
                .toList();

        return new PageResponse<>(records, page, size, total, totalPages);
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
