package com.syde.mealplanner.controller;

import com.jayway.jsonpath.JsonPath;
import com.syde.mealplanner.entity.Inventory;
import com.syde.mealplanner.entity.MealPlanDraft;
import com.syde.mealplanner.entity.ShoppingList;
import com.syde.mealplanner.entity.json.NutritionSnapshot;
import com.syde.mealplanner.entity.json.RecipeIngredientSnapshot;
import com.syde.mealplanner.entity.json.RecipeSnapshot;
import com.syde.mealplanner.mapper.InventoryMapper;
import com.syde.mealplanner.mapper.MealHistoryMapper;
import com.syde.mealplanner.mapper.MealPlanDraftMapper;
import com.syde.mealplanner.mapper.ShoppingListMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MealPlannerControllerIntegrationTests {

    private static final String RAW_PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MealPlanDraftMapper mealPlanDraftMapper;

    @Autowired
    private InventoryMapper inventoryMapper;

    @Autowired
    private ShoppingListMapper shoppingListMapper;

    @Autowired
    private MealHistoryMapper mealHistoryMapper;

    @Test
    void unauthenticatedMealPlanRequestsReturn401() throws Exception {
        mockMvc.perform(post("/api/meal-plans/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userRequest": "high protein dinners"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(get("/api/meal-plans/current"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(delete("/api/meal-plans/current"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(put("/api/meal-plans/current/recipes/{recipeId}/replace", 1001L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(post("/api/meal-plans/current/recipes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(delete("/api/meal-plans/current/recipes/{recipeId}", 1001L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(post("/api/meal-plans/current/confirm"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void generateAndGetCurrentDraftForAuthenticatedUser() throws Exception {
        String token = registerAndLogin("meal-plan-user-" + UUID.randomUUID());

        String responseBody = mockMvc.perform(post("/api/meal-plans/generate")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userRequest": "high protein dinners"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.userRequest").value("high protein dinners"))
                .andExpect(jsonPath("$.data.recipes.length()").value(5))
                .andExpect(jsonPath("$.data.recipes[0].id").value(1001))
                .andExpect(jsonPath("$.data.recipes[0].title").value("Lemon Chicken Pasta"))
                .andExpect(jsonPath("$.data.recipes[0].ingredients[0].name").value("Chicken breast"))
                .andExpect(jsonPath("$.data.recipes[0].steps[0]").value("Cook pasta until tender."))
                .andExpect(jsonPath("$.data.recipes[0].nutrition.protein.amount").value(48))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number draftId = JsonPath.read(responseBody, "$.data.id");

        mockMvc.perform(get("/api/meal-plans/current")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(draftId.longValue()))
                .andExpect(jsonPath("$.data.userRequest").value("high protein dinners"))
                .andExpect(jsonPath("$.data.recipes.length()").value(5))
                .andExpect(jsonPath("$.data.recipes[0].title").value("Lemon Chicken Pasta"));
    }

    @Test
    void generateReplacesExistingCurrentDraft() throws Exception {
        String token = registerAndLogin("meal-plan-replace-" + UUID.randomUUID());

        String firstResponse = generateDraft(token, "high protein dinners");
        Number firstDraftId = JsonPath.read(firstResponse, "$.data.id");

        mockMvc.perform(post("/api/meal-plans/generate")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userRequest": "vegetarian dinners"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(firstDraftId.longValue()))
                .andExpect(jsonPath("$.data.userRequest").value("vegetarian dinners"))
                .andExpect(jsonPath("$.data.recipes.length()").value(5))
                .andExpect(jsonPath("$.data.recipes[0].title").value("Chickpea Tomato Pasta"));
    }

    @Test
    void getCurrentReturnsNullWhenNoDraftExists() throws Exception {
        String token = registerAndLogin("meal-plan-empty-" + UUID.randomUUID());

        mockMvc.perform(get("/api/meal-plans/current")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void deleteCurrentDraftDeletesDraftAndMissingDraftReturns404() throws Exception {
        String token = registerAndLogin("meal-plan-delete-" + UUID.randomUUID());
        generateDraft(token, "high protein dinners");

        mockMvc.perform(delete("/api/meal-plans/current")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/meal-plans/current")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(delete("/api/meal-plans/current")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Meal plan draft not found"));
    }

    @Test
    void generateValidationFailuresReturn400() throws Exception {
        String token = registerAndLogin("meal-plan-validation-" + UUID.randomUUID());

        mockMvc.perform(post("/api/meal-plans/generate")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userRequest": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("User request is required"));

        mockMvc.perform(post("/api/meal-plans/generate")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userRequest": "%s"
                                }
                                """.formatted("a".repeat(501))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("User request must be at most 500 characters"));
    }

    @Test
    void replaceRecipeUpdatesSameIndexPersistsAndKeepsOtherRecipes() throws Exception {
        String token = registerAndLogin("meal-plan-recipe-replace-" + UUID.randomUUID());
        String draftBody = generateDraft(token, "high protein dinners");

        Number draftId = JsonPath.read(draftBody, "$.data.id");
        Number oldRecipeId = JsonPath.read(draftBody, "$.data.recipes[1].id");
        String oldFirstTitle = JsonPath.read(draftBody, "$.data.recipes[0].title");
        Number oldThirdId = JsonPath.read(draftBody, "$.data.recipes[2].id");
        Number oldFourthId = JsonPath.read(draftBody, "$.data.recipes[3].id");
        Number oldFifthId = JsonPath.read(draftBody, "$.data.recipes[4].id");

        String replaceBody = mockMvc.perform(put("/api/meal-plans/current/recipes/{recipeId}/replace", oldRecipeId.longValue())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(draftId.longValue()))
                .andExpect(jsonPath("$.data.recipes.length()").value(5))
                .andExpect(jsonPath("$.data.recipes[0].title").value(oldFirstTitle))
                .andExpect(jsonPath("$.data.recipes[2].id").value(oldThirdId.longValue()))
                .andExpect(jsonPath("$.data.recipes[3].id").value(oldFourthId.longValue()))
                .andExpect(jsonPath("$.data.recipes[4].id").value(oldFifthId.longValue()))
                .andExpect(jsonPath("$.data.recipes[1].title").value("Mediterranean Chicken Bowl"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number replacementId = JsonPath.read(replaceBody, "$.data.recipes[1].id");
        assertNotEquals(oldRecipeId.longValue(), replacementId.longValue());
        assertUniqueRecipeIds(replaceBody);

        mockMvc.perform(get("/api/meal-plans/current")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recipes.length()").value(5))
                .andExpect(jsonPath("$.data.recipes[1].id").value(replacementId.longValue()))
                .andExpect(jsonPath("$.data.recipes[1].title").value("Mediterranean Chicken Bowl"))
                .andExpect(jsonPath("$.data.recipes[0].title").value(oldFirstTitle))
                .andExpect(jsonPath("$.data.recipes[2].id").value(oldThirdId.longValue()))
                .andExpect(jsonPath("$.data.recipes[3].id").value(oldFourthId.longValue()))
                .andExpect(jsonPath("$.data.recipes[4].id").value(oldFifthId.longValue()));
    }

    @Test
    void replaceRecipeReturns404ForMissingDraftOrRecipe() throws Exception {
        String emptyUserToken = registerAndLogin("meal-plan-replace-empty-" + UUID.randomUUID());
        mockMvc.perform(put("/api/meal-plans/current/recipes/{recipeId}/replace", 1001L)
                        .header("Authorization", bearer(emptyUserToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Meal plan draft not found"));

        String token = registerAndLogin("meal-plan-replace-missing-" + UUID.randomUUID());
        generateDraft(token, "high protein dinners");

        mockMvc.perform(put("/api/meal-plans/current/recipes/{recipeId}/replace", 999999L)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Recipe not found in meal plan draft"));
    }

    @Test
    void replaceRecipeCannotModifyAnotherUsersDraft() throws Exception {
        String ownerToken = registerAndLogin("meal-plan-replace-owner-" + UUID.randomUUID());
        String otherToken = registerAndLogin("meal-plan-replace-other-" + UUID.randomUUID());

        String ownerDraft = generateDraft(ownerToken, "vegetarian dinners");
        Number ownerRecipeId = JsonPath.read(ownerDraft, "$.data.recipes[0].id");

        generateDraft(otherToken, "high protein dinners");

        mockMvc.perform(put("/api/meal-plans/current/recipes/{recipeId}/replace", ownerRecipeId.longValue())
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Recipe not found in meal plan draft"));

        mockMvc.perform(get("/api/meal-plans/current")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recipes[0].id").value(ownerRecipeId.longValue()))
                .andExpect(jsonPath("$.data.recipes[0].title").value("Chickpea Tomato Pasta"));
    }

    @Test
    void addRecipeAppendsOneRecipePersistsAndPreservesExistingRecipes() throws Exception {
        String token = registerAndLogin("meal-plan-add-" + UUID.randomUUID());
        String draftBody = generateDraft(token, "high protein dinners");

        java.util.List<Number> originalIds = JsonPath.read(draftBody, "$.data.recipes[*].id");
        java.util.List<String> originalTitles = JsonPath.read(draftBody, "$.data.recipes[*].title");

        String firstAddBody = mockMvc.perform(post("/api/meal-plans/current/recipes")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.recipes.length()").value(6))
                .andExpect(jsonPath("$.data.recipes[5].title").value("Herb Chicken Tray Bake"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertRecipePrefixUnchanged(firstAddBody, originalIds, originalTitles);
        assertUniqueRecipeIds(firstAddBody);
        Number firstAddedId = JsonPath.read(firstAddBody, "$.data.recipes[5].id");

        String secondAddBody = mockMvc.perform(post("/api/meal-plans/current/recipes")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recipes.length()").value(7))
                .andExpect(jsonPath("$.data.recipes[5].id").value(firstAddedId.longValue()))
                .andExpect(jsonPath("$.data.recipes[6].title").value("Cod Potato Skillet"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertRecipePrefixUnchanged(secondAddBody, originalIds, originalTitles);
        assertUniqueRecipeIds(secondAddBody);

        mockMvc.perform(get("/api/meal-plans/current")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recipes.length()").value(7))
                .andExpect(jsonPath("$.data.recipes[5].id").value(firstAddedId.longValue()))
                .andExpect(jsonPath("$.data.recipes[6].title").value("Cod Potato Skillet"));
    }

    @Test
    void addRecipeReturns404WhenNoDraftExists() throws Exception {
        String token = registerAndLogin("meal-plan-add-empty-" + UUID.randomUUID());

        mockMvc.perform(post("/api/meal-plans/current/recipes")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Meal plan draft not found"));
    }

    @Test
    void addRecipeModifiesOnlyAuthenticatedUsersDraft() throws Exception {
        String userToken = registerAndLogin("meal-plan-add-user-" + UUID.randomUUID());
        String otherToken = registerAndLogin("meal-plan-add-other-" + UUID.randomUUID());

        String userDraft = generateDraft(userToken, "high protein dinners");
        String otherDraft = generateDraft(otherToken, "vegetarian dinners");
        Number otherFirstRecipeId = JsonPath.read(otherDraft, "$.data.recipes[0].id");

        mockMvc.perform(post("/api/meal-plans/current/recipes")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recipes.length()").value(6));

        mockMvc.perform(get("/api/meal-plans/current")
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recipes.length()").value(5))
                .andExpect(jsonPath("$.data.recipes[0].id").value(otherFirstRecipeId.longValue()))
                .andExpect(jsonPath("$.data.recipes[0].title").value("Chickpea Tomato Pasta"));

        mockMvc.perform(get("/api/meal-plans/current")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(((Number) JsonPath.read(userDraft, "$.data.id")).longValue()))
                .andExpect(jsonPath("$.data.recipes.length()").value(6));
    }

    @Test
    void replaceRecipePreservesCurrentRecipeCountAfterAdd() throws Exception {
        String token = registerAndLogin("meal-plan-add-then-replace-" + UUID.randomUUID());
        generateDraft(token, "high protein dinners");
        String addedBody = mockMvc.perform(post("/api/meal-plans/current/recipes")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recipes.length()").value(6))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number recipeToReplaceId = JsonPath.read(addedBody, "$.data.recipes[5].id");
        String replaceBody = mockMvc.perform(put("/api/meal-plans/current/recipes/{recipeId}/replace", recipeToReplaceId.longValue())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recipes.length()").value(6))
                .andExpect(jsonPath("$.data.recipes[5].title").value("Mediterranean Chicken Bowl"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertUniqueRecipeIds(replaceBody);
    }

    @Test
    void deleteRecipeRemovesOneRecipePreservesOrderAndPersists() throws Exception {
        String token = registerAndLogin("meal-plan-delete-recipe-" + UUID.randomUUID());
        String draftBody = generateDraft(token, "high protein dinners");

        java.util.List<Number> originalIds = JsonPath.read(draftBody, "$.data.recipes[*].id");
        java.util.List<String> originalTitles = JsonPath.read(draftBody, "$.data.recipes[*].title");
        Number recipeToDelete = originalIds.get(2);

        String deleteBody = mockMvc.perform(delete("/api/meal-plans/current/recipes/{recipeId}", recipeToDelete.longValue())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.recipes.length()").value(4))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertRecipeRemovedAndOrderPreserved(deleteBody, originalIds, originalTitles, 2);

        mockMvc.perform(get("/api/meal-plans/current")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recipes.length()").value(4))
                .andExpect(jsonPath("$.data.recipes[0].id").value(originalIds.get(0).longValue()))
                .andExpect(jsonPath("$.data.recipes[1].id").value(originalIds.get(1).longValue()))
                .andExpect(jsonPath("$.data.recipes[2].id").value(originalIds.get(3).longValue()))
                .andExpect(jsonPath("$.data.recipes[3].id").value(originalIds.get(4).longValue()));
    }

    @Test
    void deleteRecipeFromDraftWithMoreThanFiveRecipesDecreasesCountByOne() throws Exception {
        String token = registerAndLogin("meal-plan-delete-added-" + UUID.randomUUID());
        generateDraft(token, "high protein dinners");
        String addedBody = mockMvc.perform(post("/api/meal-plans/current/recipes")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recipes.length()").value(6))
                .andReturn()
                .getResponse()
                .getContentAsString();

        java.util.List<Number> idsBeforeDelete = JsonPath.read(addedBody, "$.data.recipes[*].id");
        java.util.List<String> titlesBeforeDelete = JsonPath.read(addedBody, "$.data.recipes[*].title");
        Number recipeToDelete = idsBeforeDelete.get(5);

        String deleteBody = mockMvc.perform(delete("/api/meal-plans/current/recipes/{recipeId}", recipeToDelete.longValue())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recipes.length()").value(5))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertRecipeRemovedAndOrderPreserved(deleteBody, idsBeforeDelete, titlesBeforeDelete, 5);
    }

    @Test
    void deleteRecipeReturns404ForMissingDraftOrRecipe() throws Exception {
        String emptyUserToken = registerAndLogin("meal-plan-delete-recipe-empty-" + UUID.randomUUID());
        mockMvc.perform(delete("/api/meal-plans/current/recipes/{recipeId}", 1001L)
                        .header("Authorization", bearer(emptyUserToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Meal plan draft not found"));

        String token = registerAndLogin("meal-plan-delete-recipe-missing-" + UUID.randomUUID());
        generateDraft(token, "high protein dinners");

        mockMvc.perform(delete("/api/meal-plans/current/recipes/{recipeId}", 999999L)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Recipe not found in meal plan draft"));
    }

    @Test
    void deleteLastRecipeKeepsDraftWithEmptyRecipeList() throws Exception {
        String token = registerAndLogin("meal-plan-delete-last-recipe-" + UUID.randomUUID());
        String currentBody = generateDraft(token, "high protein dinners");

        for (int remaining = 5; remaining > 0; remaining--) {
            Number recipeId = JsonPath.read(currentBody, "$.data.recipes[0].id");
            currentBody = mockMvc.perform(delete("/api/meal-plans/current/recipes/{recipeId}", recipeId.longValue())
                            .header("Authorization", bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").isNumber())
                    .andExpect(jsonPath("$.data.recipes.length()").value(remaining - 1))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
        }

        mockMvc.perform(get("/api/meal-plans/current")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.recipes.length()").value(0));
    }

    @Test
    void deleteRecipeCannotModifyAnotherUsersDraft() throws Exception {
        String ownerToken = registerAndLogin("meal-plan-delete-recipe-owner-" + UUID.randomUUID());
        String otherToken = registerAndLogin("meal-plan-delete-recipe-other-" + UUID.randomUUID());

        String ownerDraft = generateDraft(ownerToken, "vegetarian dinners");
        Number ownerRecipeId = JsonPath.read(ownerDraft, "$.data.recipes[0].id");
        generateDraft(otherToken, "high protein dinners");

        mockMvc.perform(delete("/api/meal-plans/current/recipes/{recipeId}", ownerRecipeId.longValue())
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Recipe not found in meal plan draft"));

        mockMvc.perform(get("/api/meal-plans/current")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recipes.length()").value(5))
                .andExpect(jsonPath("$.data.recipes[0].id").value(ownerRecipeId.longValue()))
                .andExpect(jsonPath("$.data.recipes[0].title").value("Chickpea Tomato Pasta"));
    }

    @Test
    void confirmCurrentDraftCreatesHistoryMergesShoppingListAndDeletesDraft() throws Exception {
        TestUser user = registerAndLoginUser("meal-plan-confirm-" + UUID.randomUUID());
        insertInventory(user.id(), " chicken breast ", "0.50", "KG");
        Inventory riceInventory = insertInventory(user.id(), "Rice", "0.25", "kg");
        ShoppingList existingRice = insertShoppingList(user.id(), " rice ", "1.00", "0.00", "1.00", " KG ");
        insertShoppingList(user.id(), "Milk", "2.00", "0.00", "2.00", "L");
        MealPlanDraft draft = insertDraft(user.id(), "confirm protein meals", List.of(
                recipe(7001L, "Chicken Dinner", List.of(
                        ingredient("Chicken Breast", "0.40", "kg"),
                        ingredient("Rice", "1.00", "kg"))),
                recipe(7002L, "Chicken Lunch", List.of(
                        ingredient(" chicken breast ", "0.30", "KG"),
                        ingredient("Rice", "0.50", "kg"),
                        ingredient("Tomato", "0.20", "kg")))));

        String responseBody = mockMvc.perform(post("/api/meal-plans/current/confirm")
                        .header("Authorization", bearer(user.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.userId").value(user.id().longValue()))
                .andExpect(jsonPath("$.data.userRequest").value("confirm protein meals"))
                .andExpect(jsonPath("$.data.recipes.length()").value(2))
                .andExpect(jsonPath("$.data.shoppingListSnapshot.length()").value(3))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number historyId = JsonPath.read(responseBody, "$.data.id");
        assertEquals(1, mealHistoryMapper.selectAllByUserId(user.id()).size());
        assertEquals(historyId.longValue(), mealHistoryMapper.selectByIdAndUserId(historyId.longValue(), user.id()).getId());
        assertEquals("Chicken Dinner", JsonPath.read(responseBody, "$.data.recipes[0].title"));

        assertSnapshotItem(responseBody, "Chicken Breast", "0.70", "0.50", "0.20", "kg");
        assertSnapshotItem(responseBody, "Rice", "1.50", "0.25", "1.25", "kg");
        assertSnapshotItem(responseBody, "Tomato", "0.20", "0", "0.20", "kg");

        ShoppingList mergedRice = shoppingListMapper.selectByIdAndUserId(existingRice.getId(), user.id());
        assertBigDecimalEquals("2.50", mergedRice.getRequiredQuantity());
        assertBigDecimalEquals("0.25", mergedRice.getAvailableQuantity());
        assertBigDecimalEquals("2.25", mergedRice.getQuantityToBuy());
        assertEquals(4, shoppingListMapper.selectAllByUserId(user.id()).size());
        assertBigDecimalEquals("0.25", inventoryMapper.selectByIdAndUserId(riceInventory.getId(), user.id()).getQuantity());
        assertEquals(0, mealPlanDraftMapper.deleteByUserId(999999999L));
        assertEquals(null, mealPlanDraftMapper.selectByIdAndUserId(draft.getId(), user.id()));

        mockMvc.perform(get("/api/meal-plans/current")
                        .header("Authorization", bearer(user.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void confirmCurrentDraftWithEmptyRecipesCreatesEmptyHistoryAndDeletesDraft() throws Exception {
        TestUser user = registerAndLoginUser("meal-plan-confirm-empty-" + UUID.randomUUID());
        MealPlanDraft draft = insertDraft(user.id(), "empty draft", List.of());

        mockMvc.perform(post("/api/meal-plans/current/confirm")
                        .header("Authorization", bearer(user.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userRequest").value("empty draft"))
                .andExpect(jsonPath("$.data.recipes.length()").value(0))
                .andExpect(jsonPath("$.data.shoppingListSnapshot.length()").value(0));

        assertEquals(1, mealHistoryMapper.selectAllByUserId(user.id()).size());
        assertEquals(null, mealPlanDraftMapper.selectByIdAndUserId(draft.getId(), user.id()));
        assertEquals(0, shoppingListMapper.selectAllByUserId(user.id()).size());
    }

    @Test
    void confirmCurrentDraftReturns404WhenNoDraftExists() throws Exception {
        String token = registerAndLogin("meal-plan-confirm-missing-" + UUID.randomUUID());

        mockMvc.perform(post("/api/meal-plans/current/confirm")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Meal plan draft not found"));
    }

    @Test
    void confirmCurrentDraftPreservesCrossUserIsolation() throws Exception {
        TestUser user = registerAndLoginUser("meal-plan-confirm-user-" + UUID.randomUUID());
        TestUser otherUser = registerAndLoginUser("meal-plan-confirm-other-" + UUID.randomUUID());
        insertDraft(user.id(), "user draft", List.of(recipe(7101L, "User Recipe", List.of(ingredient("Rice", "1.00", "kg")))));
        MealPlanDraft otherDraft = insertDraft(otherUser.id(), "other draft", List.of(recipe(7201L, "Other Recipe", List.of(ingredient("Rice", "2.00", "kg")))));
        ShoppingList otherShoppingList = insertShoppingList(otherUser.id(), "Rice", "3.00", "0.00", "3.00", "kg");

        mockMvc.perform(post("/api/meal-plans/current/confirm")
                        .header("Authorization", bearer(user.token())))
                .andExpect(status().isOk());

        assertEquals(1, mealHistoryMapper.selectAllByUserId(user.id()).size());
        assertEquals(0, mealHistoryMapper.selectAllByUserId(otherUser.id()).size());
        assertEquals(null, mealPlanDraftMapper.selectByUserId(user.id()));
        assertEquals(otherDraft.getId(), mealPlanDraftMapper.selectByUserId(otherUser.id()).getId());
        assertEquals(otherShoppingList.getId(), shoppingListMapper.selectByIdAndUserId(otherShoppingList.getId(), otherUser.id()).getId());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void confirmCurrentDraftRollsBackWhenShoppingListWriteFails() throws Exception {
        TestUser user = registerAndLoginUser("meal-plan-confirm-rollback-" + UUID.randomUUID());
        MealPlanDraft draft = insertDraft(user.id(), "rollback draft", List.of(
                recipe(7301L, "Valid Recipe", List.of(ingredient("Rice", "1.00", "kg"))),
                recipe(7302L, "Invalid Recipe", List.of(ingredient("x".repeat(150), "1.00", "kg")))));

        mockMvc.perform(post("/api/meal-plans/current/confirm")
                        .header("Authorization", bearer(user.token())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500));

        assertEquals(draft.getId(), mealPlanDraftMapper.selectByUserId(user.id()).getId());
        assertEquals(0, mealHistoryMapper.selectAllByUserId(user.id()).size());
        assertEquals(0, shoppingListMapper.selectAllByUserId(user.id()).size());

        mealPlanDraftMapper.deleteByUserId(user.id());
    }

    private String generateDraft(String token, String userRequest) throws Exception {
        return mockMvc.perform(post("/api/meal-plans/generate")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userRequest": "%s"
                                }
                                """.formatted(userRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private String registerAndLogin(String username) throws Exception {
        String safeUsername = username.length() > 40 ? username.substring(0, 40) : username;
        String email = "meal-plan-test-" + UUID.randomUUID() + "@example.com";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(safeUsername, email, RAW_PASSWORD)))
                .andExpect(status().isOk());

        String responseBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, RAW_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return JsonPath.read(responseBody, "$.data.token");
    }

    private TestUser registerAndLoginUser(String username) throws Exception {
        String safeUsername = username.length() > 40 ? username.substring(0, 40) : username;
        String email = "meal-plan-test-" + UUID.randomUUID() + "@example.com";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(safeUsername, email, RAW_PASSWORD)))
                .andExpect(status().isOk());

        String responseBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, RAW_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = JsonPath.read(responseBody, "$.data.token");
        Number userId = JsonPath.read(responseBody, "$.data.user.id");
        return new TestUser(token, userId.longValue());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void assertUniqueRecipeIds(String responseBody) {
        java.util.List<Number> recipeIds = JsonPath.read(responseBody, "$.data.recipes[*].id");
        long uniqueCount = recipeIds.stream()
                .map(Number::longValue)
                .distinct()
                .count();
        assertEquals(recipeIds.size(), uniqueCount);
    }

    private void assertRecipePrefixUnchanged(
            String responseBody,
            java.util.List<Number> expectedIds,
            java.util.List<String> expectedTitles) {
        java.util.List<Number> actualIds = JsonPath.read(responseBody, "$.data.recipes[*].id");
        java.util.List<String> actualTitles = JsonPath.read(responseBody, "$.data.recipes[*].title");
        for (int index = 0; index < expectedIds.size(); index++) {
            assertEquals(expectedIds.get(index).longValue(), actualIds.get(index).longValue());
            assertEquals(expectedTitles.get(index), actualTitles.get(index));
        }
    }

    private void assertRecipeRemovedAndOrderPreserved(
            String responseBody,
            java.util.List<Number> originalIds,
            java.util.List<String> originalTitles,
            int removedIndex) {
        java.util.List<Number> actualIds = JsonPath.read(responseBody, "$.data.recipes[*].id");
        java.util.List<String> actualTitles = JsonPath.read(responseBody, "$.data.recipes[*].title");
        assertEquals(originalIds.size() - 1, actualIds.size());

        int actualIndex = 0;
        for (int originalIndex = 0; originalIndex < originalIds.size(); originalIndex++) {
            if (originalIndex == removedIndex) {
                continue;
            }
            assertEquals(originalIds.get(originalIndex).longValue(), actualIds.get(actualIndex).longValue());
            assertEquals(originalTitles.get(originalIndex), actualTitles.get(actualIndex));
            actualIndex++;
        }
    }

    private MealPlanDraft insertDraft(Long userId, String userRequest, List<RecipeSnapshot> recipes) {
        MealPlanDraft draft = new MealPlanDraft();
        draft.setUserId(userId);
        draft.setUserRequest(userRequest);
        draft.setRecipes(recipes);
        assertEquals(1, mealPlanDraftMapper.insert(draft));
        return draft;
    }

    private Inventory insertInventory(Long userId, String foodName, String quantity, String unit) {
        Inventory inventory = new Inventory();
        inventory.setUserId(userId);
        inventory.setFoodName(foodName);
        inventory.setQuantity(new BigDecimal(quantity));
        inventory.setUnit(unit);
        inventory.setAddedDate(LocalDate.now());
        inventory.setReminderDays(0);
        assertEquals(1, inventoryMapper.insert(inventory));
        return inventory;
    }

    private ShoppingList insertShoppingList(
            Long userId,
            String foodName,
            String requiredQuantity,
            String availableQuantity,
            String quantityToBuy,
            String unit) {
        ShoppingList item = new ShoppingList();
        item.setUserId(userId);
        item.setFoodName(foodName);
        item.setRequiredQuantity(new BigDecimal(requiredQuantity));
        item.setAvailableQuantity(new BigDecimal(availableQuantity));
        item.setQuantityToBuy(new BigDecimal(quantityToBuy));
        item.setUnit(unit);
        assertEquals(1, shoppingListMapper.insert(item));
        return item;
    }

    private RecipeSnapshot recipe(Long id, String title, List<RecipeIngredientSnapshot> ingredients) {
        NutritionSnapshot protein = new NutritionSnapshot();
        protein.setAmount(new BigDecimal("20"));
        protein.setUnit("g");
        protein.setDailyValuePercent(new BigDecimal("40"));

        RecipeSnapshot recipe = new RecipeSnapshot();
        recipe.setId(id);
        recipe.setTitle(title);
        recipe.setIngredients(ingredients);
        recipe.setSteps(List.of("Prepare ingredients.", "Cook until done."));
        recipe.setNutrition(Map.of("protein", protein));
        return recipe;
    }

    private RecipeIngredientSnapshot ingredient(String name, String quantity, String unit) {
        RecipeIngredientSnapshot ingredient = new RecipeIngredientSnapshot();
        ingredient.setName(name);
        ingredient.setQuantity(new BigDecimal(quantity));
        ingredient.setUnit(unit);
        return ingredient;
    }

    private void assertSnapshotItem(
            String responseBody,
            String name,
            String requiredQuantity,
            String availableQuantity,
            String quantityToBuy,
            String unit) {
        List<Map<String, Object>> items = JsonPath.read(responseBody, "$.data.shoppingListSnapshot");
        Map<String, Object> item = items.stream()
                .filter(candidate -> name.equals(candidate.get("name")))
                .findFirst()
                .orElseThrow();

        assertBigDecimalEquals(requiredQuantity, new BigDecimal(item.get("requiredQuantity").toString()));
        assertBigDecimalEquals(availableQuantity, new BigDecimal(item.get("availableFromInventory").toString()));
        assertBigDecimalEquals(quantityToBuy, new BigDecimal(item.get("quantityToBuy").toString()));
        assertEquals(unit, item.get("unit"));
    }

    private void assertBigDecimalEquals(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    private record TestUser(String token, Long id) {
    }
}
