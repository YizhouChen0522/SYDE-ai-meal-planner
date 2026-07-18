package com.syde.mealplanner.provider.impl;

import com.syde.mealplanner.config.MealGenerationProperties;
import com.syde.mealplanner.entity.json.NutritionSnapshot;
import com.syde.mealplanner.entity.json.RecipeIngredientSnapshot;
import com.syde.mealplanner.entity.json.RecipeSnapshot;
import com.syde.mealplanner.exception.BusinessException;
import com.syde.mealplanner.provider.MealGenerationProvider;
import com.syde.mealplanner.util.JsonMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
@ConditionalOnProperty(prefix = "meal-generation", name = "provider", havingValue = "openai")
public class OpenAiMealGenerationProvider implements MealGenerationProvider {

    private static final int INITIAL_RECIPE_COUNT = 5;
    private static final String SAFE_GENERATION_FAILURE_MESSAGE = "Meal generation failed";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final MealGenerationProperties properties;
    private final OpenAiResponsesClient responsesClient;

    @Autowired
    public OpenAiMealGenerationProvider(
            MealGenerationProperties properties,
            RestClient.Builder restClientBuilder) {
        this(properties, createResponsesClient(properties, restClientBuilder));
    }

    OpenAiMealGenerationProvider(MealGenerationProperties properties, OpenAiResponsesClient responsesClient) {
        this.properties = properties;
        this.responsesClient = responsesClient;
    }

    @Override
    public List<RecipeSnapshot> generate(String userRequest) {
        return requestRecipes(
                initialPrompt(userRequest),
                INITIAL_RECIPE_COUNT,
                List.of(),
                "initial_meal_plan");
    }

    @Override
    public RecipeSnapshot generateReplacement(
            String userRequest,
            RecipeSnapshot recipeToReplace,
            List<RecipeSnapshot> currentRecipes) {
        List<RecipeSnapshot> safeCurrentRecipes = currentRecipes == null ? List.of() : currentRecipes;
        List<RecipeSnapshot> recipes = requestRecipes(
                replacementPrompt(userRequest, recipeToReplace, safeCurrentRecipes),
                1,
                safeCurrentRecipes,
                "replacement_recipe");
        RecipeSnapshot replacement = recipes.get(0);
        if (recipeToReplace != null && sameTitle(replacement, recipeToReplace)) {
            throw invalidResponseException();
        }
        return replacement;
    }

    @Override
    public RecipeSnapshot generateAdditionalRecipe(String userRequest, List<RecipeSnapshot> currentRecipes) {
        List<RecipeSnapshot> safeCurrentRecipes = currentRecipes == null ? List.of() : currentRecipes;
        return requestRecipes(
                additionalPrompt(userRequest, safeCurrentRecipes),
                1,
                safeCurrentRecipes,
                "additional_recipe").get(0);
    }

    private List<RecipeSnapshot> requestRecipes(
            String userPrompt,
            int expectedCount,
            List<RecipeSnapshot> currentRecipes,
            String schemaName) {
        try {
            String responseBody = responsesClient.createResponse(buildRequest(userPrompt, expectedCount, schemaName));
            String generatedJson = extractOutputText(responseBody);
            GeneratedRecipesResponse response = JsonMapper.getObjectMapper()
                    .readValue(generatedJson, GeneratedRecipesResponse.class);
            List<RecipeSnapshot> recipes = response.getRecipes();
            if (recipes == null || recipes.size() != expectedCount) {
                throw invalidResponseException();
            }

            recipes.forEach(this::validateRecipeContent);
            assignSafeIds(recipes, currentRecipes);
            validateUniqueIds(recipes);
            return List.copyOf(recipes);
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            throw toProviderException(exception);
        } catch (RestClientException exception) {
            throw new BusinessException(503, SAFE_GENERATION_FAILURE_MESSAGE, exception);
        } catch (JacksonException | IllegalArgumentException exception) {
            throw invalidResponseException(exception);
        } catch (RuntimeException exception) {
            throw new BusinessException(503, SAFE_GENERATION_FAILURE_MESSAGE, exception);
        }
    }

    private Map<String, Object> buildRequest(String userPrompt, int expectedCount, String schemaName) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", requireModel());
        request.put("input", List.of(
                message("system", systemPrompt()),
                message("user", userPrompt)));
        request.put("text", Map.of(
                "format", Map.of(
                        "type", "json_schema",
                        "name", schemaName,
                        "strict", true,
                        "schema", responseSchema(expectedCount))));
        return request;
    }

    private String systemPrompt() {
        return """
                You generate practical meal-plan recipes for SYDE AI Meal Planner.
                Return only the requested JSON structure. Use numeric ingredient quantities.
                Use inventory-compatible units such as kg, L, g, ml, piece, tbsp, and tsp.
                Do not use vague quantities such as "some", "to taste", or "as needed".
                """;
    }

    private String initialPrompt(String userRequest) {
        return """
                User request:
                %s

                Generate exactly five distinct recipes that satisfy the request.
                """.formatted(safeText(userRequest));
    }

    private String replacementPrompt(
            String userRequest,
            RecipeSnapshot recipeToReplace,
            List<RecipeSnapshot> currentRecipes) {
        return """
                Original user request:
                %s

                Replace this recipe with a different recipe:
                %s

                Current draft recipes:
                %s

                Generate exactly one replacement recipe. Avoid duplicating the other current recipes.
                """.formatted(safeText(userRequest), toJson(recipeToReplace), toJson(currentRecipes));
    }

    private String additionalPrompt(String userRequest, List<RecipeSnapshot> currentRecipes) {
        return """
                Original user request:
                %s

                Current draft recipes:
                %s

                Generate exactly one additional recipe that fits the request and does not duplicate the current recipes.
                """.formatted(safeText(userRequest), toJson(currentRecipes));
    }

    private Map<String, Object> responseSchema(int expectedCount) {
        Map<String, Object> root = objectSchema();
        root.put("properties", Map.of(
                "recipes", Map.of(
                        "type", "array",
                        "minItems", expectedCount,
                        "maxItems", expectedCount,
                        "items", recipeSchema())));
        root.put("required", List.of("recipes"));
        return root;
    }

    private Map<String, Object> recipeSchema() {
        Map<String, Object> schema = objectSchema();
        schema.put("properties", Map.of(
                "id", Map.of("type", "integer"),
                "title", Map.of("type", "string", "minLength", 1),
                "ingredients", Map.of(
                        "type", "array",
                        "minItems", 1,
                        "items", ingredientSchema()),
                "steps", Map.of(
                        "type", "array",
                        "minItems", 1,
                        "items", Map.of("type", "string", "minLength", 1)),
                "nutrition", nutritionSchema()));
        schema.put("required", List.of("id", "title", "ingredients", "steps", "nutrition"));
        return schema;
    }

    private Map<String, Object> ingredientSchema() {
        Map<String, Object> schema = objectSchema();
        schema.put("properties", Map.of(
                "name", Map.of("type", "string", "minLength", 1),
                "quantity", Map.of("type", "number", "exclusiveMinimum", 0),
                "unit", Map.of("type", "string", "minLength", 1)));
        schema.put("required", List.of("name", "quantity", "unit"));
        return schema;
    }

    private Map<String, Object> nutritionSchema() {
        Map<String, Object> schema = objectSchema();
        schema.put("properties", Map.of(
                "calories", nutritionValueSchema(),
                "protein", nutritionValueSchema(),
                "carbohydrates", nutritionValueSchema(),
                "fat", nutritionValueSchema()));
        schema.put("required", List.of("calories", "protein", "carbohydrates", "fat"));
        return schema;
    }

    private Map<String, Object> nutritionValueSchema() {
        Map<String, Object> schema = objectSchema();
        schema.put("properties", Map.of(
                "amount", Map.of("type", "number", "minimum", 0),
                "unit", Map.of("type", "string", "minLength", 1),
                "dailyValuePercent", Map.of("type", "number", "minimum", 0)));
        schema.put("required", List.of("amount", "unit", "dailyValuePercent"));
        return schema;
    }

    private Map<String, Object> objectSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        return schema;
    }

    private Map<String, String> message(String role, String content) {
        return Map.of("role", role, "content", content);
    }

    private String extractOutputText(String responseBody) {
        try {
            Map<String, Object> root = JsonMapper.getObjectMapper().readValue(responseBody, MAP_TYPE);
            Object directOutputText = root.get("output_text");
            if (directOutputText instanceof String text && !text.isBlank()) {
                return text;
            }

            Object output = root.get("output");
            if (output instanceof List<?> outputItems) {
                for (Object outputItem : outputItems) {
                    if (!(outputItem instanceof Map<?, ?> outputMap)) {
                        continue;
                    }
                    Object content = outputMap.get("content");
                    if (!(content instanceof List<?> contentItems)) {
                        continue;
                    }
                    for (Object contentItem : contentItems) {
                        if (contentItem instanceof Map<?, ?> contentMap
                                && contentMap.get("text") instanceof String text
                                && !text.isBlank()) {
                            return text;
                        }
                    }
                }
            }
        } catch (JacksonException exception) {
            throw invalidResponseException(exception);
        }

        throw invalidResponseException();
    }

    private void assignSafeIds(List<RecipeSnapshot> recipes, List<RecipeSnapshot> currentRecipes) {
        Set<Long> usedIds = new HashSet<>();
        if (currentRecipes != null) {
            currentRecipes.stream()
                    .map(RecipeSnapshot::getId)
                    .filter(Objects::nonNull)
                    .forEach(usedIds::add);
        }

        long nextId = usedIds.stream().max(Long::compareTo).orElse(0L) + 1L;
        for (RecipeSnapshot recipe : recipes) {
            while (usedIds.contains(nextId)) {
                nextId++;
            }
            recipe.setId(nextId);
            usedIds.add(nextId);
            nextId++;
        }
    }

    private void validateRecipeContent(RecipeSnapshot recipe) {
        if (recipe == null || isBlank(recipe.getTitle())) {
            throw invalidResponseException();
        }

        if (recipe.getIngredients() == null || recipe.getIngredients().isEmpty()) {
            throw invalidResponseException();
        }
        for (RecipeIngredientSnapshot ingredient : recipe.getIngredients()) {
            if (ingredient == null
                    || isBlank(ingredient.getName())
                    || ingredient.getQuantity() == null
                    || ingredient.getQuantity().compareTo(BigDecimal.ZERO) <= 0
                    || isBlank(ingredient.getUnit())) {
                throw invalidResponseException();
            }
        }

        if (recipe.getSteps() == null || recipe.getSteps().isEmpty()
                || recipe.getSteps().stream().anyMatch(this::isBlank)) {
            throw invalidResponseException();
        }

        if (recipe.getNutrition() == null || recipe.getNutrition().isEmpty()) {
            throw invalidResponseException();
        }
        for (Map.Entry<String, NutritionSnapshot> entry : recipe.getNutrition().entrySet()) {
            NutritionSnapshot nutrition = entry.getValue();
            if (isBlank(entry.getKey())
                    || nutrition == null
                    || nutrition.getAmount() == null
                    || nutrition.getAmount().compareTo(BigDecimal.ZERO) < 0
                    || isBlank(nutrition.getUnit())
                    || nutrition.getDailyValuePercent() == null
                    || nutrition.getDailyValuePercent().compareTo(BigDecimal.ZERO) < 0) {
                throw invalidResponseException();
            }
        }
    }

    private void validateUniqueIds(List<RecipeSnapshot> recipes) {
        Set<Long> recipeIds = new HashSet<>();
        for (RecipeSnapshot recipe : recipes) {
            if (recipe.getId() == null || !recipeIds.add(recipe.getId())) {
                throw invalidResponseException();
            }
        }
    }

    private BusinessException toProviderException(RestClientResponseException exception) {
        int statusCode = exception.getStatusCode().value();
        if (statusCode == 401 || statusCode == 403) {
            return new BusinessException(502, "Meal generation provider authentication failed", exception);
        }
        if (statusCode == 429 || statusCode >= 500) {
            return new BusinessException(503, SAFE_GENERATION_FAILURE_MESSAGE, exception);
        }
        return new BusinessException(502, SAFE_GENERATION_FAILURE_MESSAGE, exception);
    }

    private BusinessException invalidResponseException() {
        return invalidResponseException(null);
    }

    private BusinessException invalidResponseException(Throwable cause) {
        return new BusinessException(502, "Meal generation provider returned invalid data", cause);
    }

    private String requireModel() {
        String model = properties.getOpenai().getModel();
        if (isBlank(model)) {
            throw new BusinessException(500, "OpenAI model is not configured");
        }
        return model.trim();
    }

    private String toJson(Object value) {
        try {
            return JsonMapper.getObjectMapper().writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new BusinessException(500, SAFE_GENERATION_FAILURE_MESSAGE, exception);
        }
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean sameTitle(RecipeSnapshot first, RecipeSnapshot second) {
        return first != null
                && second != null
                && first.getTitle() != null
                && second.getTitle() != null
                && first.getTitle().trim().equalsIgnoreCase(second.getTitle().trim());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static OpenAiResponsesClient createResponsesClient(
            MealGenerationProperties properties,
            RestClient.Builder restClientBuilder) {
        MealGenerationProperties.OpenAi openAi = properties.getOpenai();
        String apiKey = openAi.getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("OPENAI_API_KEY must be configured when meal-generation.provider=openai");
        }
        String baseUrl = openAi.getBaseUrl() == null || openAi.getBaseUrl().trim().isEmpty()
                ? "https://api.openai.com/v1"
                : openAi.getBaseUrl().trim();

        RestClient restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        return new RestClientOpenAiResponsesClient(restClient);
    }

    interface OpenAiResponsesClient {

        String createResponse(Map<String, Object> request);
    }

    private static class RestClientOpenAiResponsesClient implements OpenAiResponsesClient {

        private final RestClient restClient;

        private RestClientOpenAiResponsesClient(RestClient restClient) {
            this.restClient = restClient;
        }

        @Override
        public String createResponse(Map<String, Object> request) {
            return restClient.post()
                    .uri("/responses")
                    .body(request)
                    .retrieve()
                    .body(String.class);
        }
    }

    public static class GeneratedRecipesResponse {

        private List<RecipeSnapshot> recipes;

        public List<RecipeSnapshot> getRecipes() {
            return recipes;
        }

        public void setRecipes(List<RecipeSnapshot> recipes) {
            this.recipes = recipes;
        }
    }
}
