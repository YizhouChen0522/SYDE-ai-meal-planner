package com.syde.mealplanner.dto;

import java.util.List;

public record PageResponse<T>(
        List<T> records,
        int page,
        int size,
        long total,
        int totalPages) {
}
