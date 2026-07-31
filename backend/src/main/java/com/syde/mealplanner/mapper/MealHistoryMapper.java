package com.syde.mealplanner.mapper;

import com.syde.mealplanner.entity.MealHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MealHistoryMapper {

    int insert(MealHistory mealHistory);

    MealHistory selectByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    List<MealHistory> selectAllByUserId(@Param("userId") Long userId);

    List<MealHistory> selectPageByUserId(
            @Param("userId") Long userId,
            @Param("limit") int limit,
            @Param("offset") int offset);

    long countByUserId(@Param("userId") Long userId);
}
