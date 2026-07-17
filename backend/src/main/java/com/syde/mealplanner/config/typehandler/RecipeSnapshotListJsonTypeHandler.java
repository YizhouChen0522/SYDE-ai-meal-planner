package com.syde.mealplanner.config.typehandler;

import com.syde.mealplanner.entity.json.RecipeSnapshot;
import tools.jackson.core.type.TypeReference;

import java.util.List;

public class RecipeSnapshotListJsonTypeHandler extends AbstractJsonTypeHandler<List<RecipeSnapshot>> {

    public RecipeSnapshotListJsonTypeHandler() {
        super(new TypeReference<List<RecipeSnapshot>>() {
        });
    }
}
