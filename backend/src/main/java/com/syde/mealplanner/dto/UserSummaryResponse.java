package com.syde.mealplanner.dto;

import java.time.LocalDateTime;

public record UserSummaryResponse(
        Long id,
        String username,
        String email,
        Integer status,
        LocalDateTime createTime) {
}
