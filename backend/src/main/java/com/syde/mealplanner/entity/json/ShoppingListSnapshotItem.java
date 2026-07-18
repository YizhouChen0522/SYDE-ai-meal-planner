package com.syde.mealplanner.entity.json;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ShoppingListSnapshotItem {

    private String id;
    private String name;
    private BigDecimal requiredQuantity;
    private BigDecimal availableFromInventory;
    private BigDecimal quantityToBuy;
    private String unit;
}
