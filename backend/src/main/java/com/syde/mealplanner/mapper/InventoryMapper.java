package com.syde.mealplanner.mapper;

import com.syde.mealplanner.entity.Inventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface InventoryMapper {

    int insert(Inventory inventory);

    Inventory selectByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    List<Inventory> selectAllByUserId(@Param("userId") Long userId);

    Inventory selectByUserIdAndFoodNameAndUnit(
            @Param("userId") Long userId,
            @Param("foodName") String foodName,
            @Param("unit") String unit);

    int updateQuantityByIdAndUserId(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("quantity") BigDecimal quantity);

    int updateDetailsByIdAndUserId(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("quantity") BigDecimal quantity,
            @Param("addedDate") LocalDate addedDate,
            @Param("reminderDays") Integer reminderDays);

    int updateQuantityAndAddedDateByIdAndUserId(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("quantity") BigDecimal quantity,
            @Param("addedDate") LocalDate addedDate);

    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
