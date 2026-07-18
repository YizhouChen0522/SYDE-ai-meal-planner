package com.syde.mealplanner.controller;

import com.jayway.jsonpath.JsonPath;
import com.syde.mealplanner.entity.Inventory;
import com.syde.mealplanner.entity.MealHistory;
import com.syde.mealplanner.entity.MealPlanDraft;
import com.syde.mealplanner.entity.ShoppingList;
import com.syde.mealplanner.entity.json.NutritionSnapshot;
import com.syde.mealplanner.entity.json.RecipeIngredientSnapshot;
import com.syde.mealplanner.entity.json.RecipeSnapshot;
import com.syde.mealplanner.entity.json.ShoppingListSnapshotItem;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MealHistoryControllerIntegrationTests {

    private static final String RAW_PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MealHistoryMapper mealHistoryMapper;

    @Autowired
    private InventoryMapper inventoryMapper;

    @Autowired
    private ShoppingListMapper shoppingListMapper;

    @Autowired
    private MealPlanDraftMapper mealPlanDraftMapper;

    @Test
    void unauthenticatedMealHistoryRequestsReturn401() throws Exception {
        mockMvc.perform(get("/api/meal-history"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(get("/api/meal-history/{id}", 1L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void authenticatedUserCanListOnlyOwnHistoryOrderedNewestFirstWithoutUserId() throws Exception {
        TestUser user = registerAndLogin("history-list-user-" + UUID.randomUUID());
        TestUser otherUser = registerAndLogin("history-list-other-" + UUID.randomUUID());
        TestUser emptyUser = registerAndLogin("history-list-empty-" + UUID.randomUUID());
        LocalDateTime sharedTime = LocalDateTime.of(2026, 7, 18, 10, 0);

        MealHistory older = insertHistory(user.id(), "older request", 1001L, sharedTime.minusDays(1));
        MealHistory sameTimeLowerId = insertHistory(user.id(), "same time lower id", 1002L, sharedTime);
        MealHistory sameTimeHigherId = insertHistory(user.id(), "same time higher id", 1003L, sharedTime);
        MealHistory otherUsersHistory = insertHistory(otherUser.id(), "private request", 2001L, sharedTime.plusDays(1));

        mockMvc.perform(get("/api/meal-history")
                        .header("Authorization", bearer(user.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].id").value(sameTimeHigherId.getId().longValue()))
                .andExpect(jsonPath("$.data[1].id").value(sameTimeLowerId.getId().longValue()))
                .andExpect(jsonPath("$.data[2].id").value(older.getId().longValue()))
                .andExpect(jsonPath("$.data[0].userRequest").value("same time higher id"))
                .andExpect(jsonPath("$.data[0].recipes", hasSize(1)))
                .andExpect(jsonPath("$.data[0].shoppingListSnapshot", hasSize(1)))
                .andExpect(jsonPath("$.data[0].confirmedTime").exists())
                .andExpect(jsonPath("$.data[0].createTime").exists())
                .andExpect(jsonPath("$.data[0].userId").doesNotExist())
                .andExpect(jsonPath("$.data[*].id", not(hasItem(otherUsersHistory.getId().intValue()))));

        mockMvc.perform(get("/api/meal-history")
                        .header("Authorization", bearer(emptyUser.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void authenticatedUserCanRetrieveOwnHistoryRecordWithoutUserId() throws Exception {
        TestUser user = registerAndLogin("history-detail-user-" + UUID.randomUUID());
        MealHistory history = insertHistory(
                user.id(),
                "detail request",
                3001L,
                LocalDateTime.of(2026, 7, 18, 11, 30));

        mockMvc.perform(get("/api/meal-history/{id}", history.getId())
                        .header("Authorization", bearer(user.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(history.getId().longValue()))
                .andExpect(jsonPath("$.data.userRequest").value("detail request"))
                .andExpect(jsonPath("$.data.recipes", hasSize(1)))
                .andExpect(jsonPath("$.data.recipes[0].id").value(3001L))
                .andExpect(jsonPath("$.data.recipes[0].title").value("History Recipe 3001"))
                .andExpect(jsonPath("$.data.recipes[0].ingredients", hasSize(1)))
                .andExpect(jsonPath("$.data.recipes[0].steps", hasSize(2)))
                .andExpect(jsonPath("$.data.recipes[0].nutrition.protein.amount").value(25))
                .andExpect(jsonPath("$.data.shoppingListSnapshot", hasSize(1)))
                .andExpect(jsonPath("$.data.shoppingListSnapshot[0].name").value("Rice"))
                .andExpect(jsonPath("$.data.shoppingListSnapshot[0].quantityToBuy").value(1.50))
                .andExpect(jsonPath("$.data.confirmedTime").exists())
                .andExpect(jsonPath("$.data.createTime").exists())
                .andExpect(jsonPath("$.data.userId").doesNotExist());
    }

    @Test
    void missingOrOtherUsersHistoryReturnsSame404() throws Exception {
        TestUser user = registerAndLogin("history-404-user-" + UUID.randomUUID());
        TestUser otherUser = registerAndLogin("history-404-other-" + UUID.randomUUID());
        MealHistory otherUsersHistory = insertHistory(
                otherUser.id(),
                "private history",
                4001L,
                LocalDateTime.of(2026, 7, 18, 12, 0));

        mockMvc.perform(get("/api/meal-history/{id}", 999999999L)
                        .header("Authorization", bearer(user.token())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Meal history not found"));

        mockMvc.perform(get("/api/meal-history/{id}", otherUsersHistory.getId())
                        .header("Authorization", bearer(user.token())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Meal history not found"));
    }

    @Test
    void queryingHistoryDoesNotModifyStoredMealPlannerState() throws Exception {
        TestUser user = registerAndLogin("history-readonly-user-" + UUID.randomUUID());
        Inventory inventory = insertInventory(user.id(), "Chicken", "2.00", "kg");
        ShoppingList shoppingList = insertShoppingList(user.id(), "Rice", "3.00", "1.00", "2.00", "kg");
        MealPlanDraft draft = insertDraft(user.id(), "draft still exists");
        MealHistory history = insertHistory(
                user.id(),
                "readonly history",
                5001L,
                LocalDateTime.of(2026, 7, 18, 13, 0));

        int historyCount = mealHistoryMapper.selectAllByUserId(user.id()).size();

        mockMvc.perform(get("/api/meal-history")
                        .header("Authorization", bearer(user.token())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/meal-history/{id}", history.getId())
                        .header("Authorization", bearer(user.token())))
                .andExpect(status().isOk());

        assertEquals(historyCount, mealHistoryMapper.selectAllByUserId(user.id()).size());
        assertEquals(inventory.getId(), inventoryMapper.selectByIdAndUserId(inventory.getId(), user.id()).getId());
        assertEquals(shoppingList.getId(), shoppingListMapper.selectByIdAndUserId(shoppingList.getId(), user.id()).getId());
        assertEquals(draft.getId(), mealPlanDraftMapper.selectByUserId(user.id()).getId());
    }

    private MealHistory insertHistory(Long userId, String userRequest, Long recipeId, LocalDateTime confirmedTime) {
        MealHistory mealHistory = new MealHistory();
        mealHistory.setUserId(userId);
        mealHistory.setUserRequest(userRequest);
        mealHistory.setRecipes(List.of(recipe(recipeId)));
        mealHistory.setShoppingListSnapshot(List.of(shoppingListSnapshotItem()));
        mealHistory.setConfirmedTime(confirmedTime);
        assertEquals(1, mealHistoryMapper.insert(mealHistory));
        assertNotNull(mealHistory.getId());
        return mealHistory;
    }

    private RecipeSnapshot recipe(Long id) {
        RecipeSnapshot recipe = new RecipeSnapshot();
        recipe.setId(id);
        recipe.setTitle("History Recipe " + id);
        recipe.setIngredients(List.of(ingredient("Chicken", "0.50", "kg")));
        recipe.setSteps(List.of("Prep ingredients", "Cook until done"));
        recipe.setNutrition(Map.of("protein", nutrition("25", "g", "50")));
        return recipe;
    }

    private RecipeIngredientSnapshot ingredient(String name, String quantity, String unit) {
        RecipeIngredientSnapshot ingredient = new RecipeIngredientSnapshot();
        ingredient.setName(name);
        ingredient.setQuantity(new BigDecimal(quantity));
        ingredient.setUnit(unit);
        return ingredient;
    }

    private NutritionSnapshot nutrition(String amount, String unit, String dailyValuePercent) {
        NutritionSnapshot nutrition = new NutritionSnapshot();
        nutrition.setAmount(new BigDecimal(amount));
        nutrition.setUnit(unit);
        nutrition.setDailyValuePercent(new BigDecimal(dailyValuePercent));
        return nutrition;
    }

    private ShoppingListSnapshotItem shoppingListSnapshotItem() {
        ShoppingListSnapshotItem item = new ShoppingListSnapshotItem();
        item.setId("rice|kg");
        item.setName("Rice");
        item.setRequiredQuantity(new BigDecimal("2.00"));
        item.setAvailableFromInventory(new BigDecimal("0.50"));
        item.setQuantityToBuy(new BigDecimal("1.50"));
        item.setUnit("kg");
        return item;
    }

    private Inventory insertInventory(Long userId, String foodName, String quantity, String unit) {
        Inventory inventory = new Inventory();
        inventory.setUserId(userId);
        inventory.setFoodName(foodName);
        inventory.setQuantity(new BigDecimal(quantity));
        inventory.setUnit(unit);
        inventory.setAddedDate(LocalDate.now());
        inventory.setReminderDays(3);
        assertEquals(1, inventoryMapper.insert(inventory));
        assertNotNull(inventory.getId());
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
        assertNotNull(item.getId());
        return item;
    }

    private MealPlanDraft insertDraft(Long userId, String userRequest) {
        MealPlanDraft draft = new MealPlanDraft();
        draft.setUserId(userId);
        draft.setUserRequest(userRequest);
        draft.setRecipes(List.of(recipe(9001L)));
        assertEquals(1, mealPlanDraftMapper.insert(draft));
        assertNotNull(draft.getId());
        return draft;
    }

    private TestUser registerAndLogin(String username) throws Exception {
        String safeUsername = username.length() > 40 ? username.substring(0, 40) : username;
        String email = "meal-history-test-" + UUID.randomUUID() + "@example.com";

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

    private record TestUser(String token, Long id) {
    }
}
