package com.syde.mealplanner.config.typehandler;

import com.syde.mealplanner.entity.json.ShoppingListSnapshotItem;
import tools.jackson.core.type.TypeReference;

import java.util.List;

public class ShoppingListSnapshotJsonTypeHandler extends AbstractJsonTypeHandler<List<ShoppingListSnapshotItem>> {

    public ShoppingListSnapshotJsonTypeHandler() {
        super(new TypeReference<List<ShoppingListSnapshotItem>>() {
        });
    }
}
