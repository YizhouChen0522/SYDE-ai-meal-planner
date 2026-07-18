package com.syde.mealplanner.mapper;

import com.syde.mealplanner.entity.MealPlanDraft;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MealPlanDraftMapper {

    int insert(MealPlanDraft mealPlanDraft);

    MealPlanDraft selectByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    MealPlanDraft selectByUserId(@Param("userId") Long userId);

    int updateByUserId(MealPlanDraft mealPlanDraft);

    int deleteByUserId(@Param("userId") Long userId);
}
