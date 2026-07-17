package com.syde.mealplanner.mapper;

import com.syde.mealplanner.entity.ShoppingList;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface ShoppingListMapper {

    int insert(ShoppingList shoppingList);

    ShoppingList selectByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    List<ShoppingList> selectAllByUserId(@Param("userId") Long userId);

    ShoppingList selectByUserIdAndFoodNameAndUnit(
            @Param("userId") Long userId,
            @Param("foodName") String foodName,
            @Param("unit") String unit);

    int updateQuantitiesByIdAndUserId(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("requiredQuantity") BigDecimal requiredQuantity,
            @Param("availableQuantity") BigDecimal availableQuantity,
            @Param("quantityToBuy") BigDecimal quantityToBuy);

    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    int deleteAllByUserId(@Param("userId") Long userId);
}
