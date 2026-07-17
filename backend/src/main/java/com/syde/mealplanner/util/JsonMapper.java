package com.syde.mealplanner.util;

import tools.jackson.databind.ObjectMapper;

public final class JsonMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonMapper() {
    }

    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }
}
