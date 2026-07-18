package com.syde.mealplanner.config.typehandler;

import tools.jackson.core.type.TypeReference;

import java.util.List;

public class ListStringJsonTypeHandler extends AbstractJsonTypeHandler<List<String>> {

    public ListStringJsonTypeHandler() {
        super(new TypeReference<List<String>>() {
        });
    }
}
