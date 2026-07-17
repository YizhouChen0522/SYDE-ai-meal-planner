package com.syde.mealplanner.mapper;

import com.syde.mealplanner.entity.Inventory;
import com.syde.mealplanner.entity.MealHistory;
import com.syde.mealplanner.entity.ShoppingList;
import com.syde.mealplanner.entity.User;
import com.syde.mealplanner.entity.UserProfile;
import com.syde.mealplanner.entity.json.NutritionSnapshot;
import com.syde.mealplanner.entity.json.RecipeIngredientSnapshot;
import com.syde.mealplanner.entity.json.RecipeSnapshot;
import com.syde.mealplanner.entity.json.ShoppingListSnapshotItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class PersistenceMapperIntegrationTests {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserProfileMapper userProfileMapper;

    @Autowired
    private InventoryMapper inventoryMapper;

    @Autowired
    private ShoppingListMapper shoppingListMapper;

    @Autowired
    private MealHistoryMapper mealHistoryMapper;

    @Test
    void userMapperSupportsInsertSelectAndStatusUpdate() {
        User user = newTestUser();

        assertEquals(1, userMapper.insert(user));
        assertNotNull(user.getId());

        User selectedById = userMapper.selectById(user.getId());
        assertUserMapping(user, selectedById, 1);

        User selectedByEmail = userMapper.selectByEmail(user.getEmail());
        assertUserMapping(user, selectedByEmail, 1);

        assertEquals(1, userMapper.updateStatusById(user.getId(), 0));

        User updated = userMapper.selectById(user.getId());
        assertUserMapping(user, updated, 0);
    }

    @Test
    void userProfileMapperRoundTripsJsonListsAndSupportsNullAndEmptyLists() {
        User user = insertTestUser();

        UserProfile profile = new UserProfile();
        profile.setUserId(user.getId());
        profile.setLikedFoods(List.of("chicken", "pasta"));
        profile.setDislikedFoods(List.of("celery"));
        profile.setAllergies(List.of());

        assertEquals(1, userProfileMapper.insert(profile));
        assertNotNull(profile.getId());

        UserProfile selected = userProfileMapper.selectByUserId(user.getId());
        assertEquals(List.of("chicken", "pasta"), selected.getLikedFoods());
        assertEquals(List.of("celery"), selected.getDislikedFoods());
        assertEquals(List.of(), selected.getAllergies());

        profile.setLikedFoods(null);
        profile.setDislikedFoods(List.of());
        profile.setAllergies(List.of("peanut", "shellfish"));

        assertEquals(1, userProfileMapper.updateByUserId(profile));

        UserProfile updated = userProfileMapper.selectByUserId(user.getId());
        assertNull(updated.getLikedFoods());
        assertEquals(List.of(), updated.getDislikedFoods());
        assertEquals(List.of("peanut", "shellfish"), updated.getAllergies());
    }

    @Test
    void inventoryMapperSupportsScopedReadsUpdatesAndDeletes() {
        User user = insertTestUser();
        User otherUser = insertTestUser();

        Inventory inventory = new Inventory();
        inventory.setUserId(user.getId());
        inventory.setFoodName(uniqueName("Tomato"));
        inventory.setQuantity(new BigDecimal("2.50"));
        inventory.setUnit("kg");
        inventory.setAddedDate(LocalDate.now());
        inventory.setReminderDays(3);

        assertEquals(1, inventoryMapper.insert(inventory));
        assertNotNull(inventory.getId());

        Inventory selectedById = inventoryMapper.selectByIdAndUserId(inventory.getId(), user.getId());
        assertInventory(inventory, selectedById, "2.50");

        List<Inventory> allByUser = inventoryMapper.selectAllByUserId(user.getId());
        assertTrue(allByUser.stream().anyMatch(item -> item.getId().equals(inventory.getId())));

        Inventory selectedByKey = inventoryMapper.selectByUserIdAndFoodNameAndUnit(
                user.getId(), inventory.getFoodName(), inventory.getUnit());
        assertInventory(inventory, selectedByKey, "2.50");

        assertEquals(0, inventoryMapper.updateQuantityByIdAndUserId(
                inventory.getId(), otherUser.getId(), new BigDecimal("9.99")));
        assertBigDecimalEquals("2.50", inventoryMapper.selectByIdAndUserId(inventory.getId(), user.getId()).getQuantity());

        assertEquals(1, inventoryMapper.updateQuantityByIdAndUserId(
                inventory.getId(), user.getId(), new BigDecimal("4.75")));
        assertBigDecimalEquals("4.75", inventoryMapper.selectByIdAndUserId(inventory.getId(), user.getId()).getQuantity());

        assertEquals(0, inventoryMapper.deleteByIdAndUserId(inventory.getId(), otherUser.getId()));
        assertNotNull(inventoryMapper.selectByIdAndUserId(inventory.getId(), user.getId()));

        assertEquals(1, inventoryMapper.deleteByIdAndUserId(inventory.getId(), user.getId()));
        assertNull(inventoryMapper.selectByIdAndUserId(inventory.getId(), user.getId()));
    }

    @Test
    void shoppingListMapperSupportsScopedReadsUpdatesAndDeletes() {
        User user = insertTestUser();
        User otherUser = insertTestUser();

        ShoppingList item = new ShoppingList();
        item.setUserId(user.getId());
        item.setFoodName(uniqueName("Rice"));
        item.setRequiredQuantity(new BigDecimal("3.00"));
        item.setAvailableQuantity(new BigDecimal("1.00"));
        item.setQuantityToBuy(new BigDecimal("2.00"));
        item.setUnit("kg");

        assertEquals(1, shoppingListMapper.insert(item));
        assertNotNull(item.getId());

        ShoppingList selectedById = shoppingListMapper.selectByIdAndUserId(item.getId(), user.getId());
        assertShoppingList(item, selectedById, "3.00", "1.00", "2.00");

        List<ShoppingList> allByUser = shoppingListMapper.selectAllByUserId(user.getId());
        assertTrue(allByUser.stream().anyMatch(savedItem -> savedItem.getId().equals(item.getId())));

        ShoppingList selectedByKey = shoppingListMapper.selectByUserIdAndFoodNameAndUnit(
                user.getId(), item.getFoodName(), item.getUnit());
        assertShoppingList(item, selectedByKey, "3.00", "1.00", "2.00");

        assertEquals(0, shoppingListMapper.updateQuantitiesByIdAndUserId(
                item.getId(), otherUser.getId(), new BigDecimal("7.00"), new BigDecimal("2.00"), new BigDecimal("5.00")));
        assertShoppingList(item, shoppingListMapper.selectByIdAndUserId(item.getId(), user.getId()), "3.00", "1.00", "2.00");

        assertEquals(1, shoppingListMapper.updateQuantitiesByIdAndUserId(
                item.getId(), user.getId(), new BigDecimal("7.00"), new BigDecimal("2.00"), new BigDecimal("5.00")));
        assertShoppingList(item, shoppingListMapper.selectByIdAndUserId(item.getId(), user.getId()), "7.00", "2.00", "5.00");

        assertEquals(0, shoppingListMapper.deleteByIdAndUserId(item.getId(), otherUser.getId()));
        assertNotNull(shoppingListMapper.selectByIdAndUserId(item.getId(), user.getId()));

        assertEquals(1, shoppingListMapper.deleteAllByUserId(user.getId()));
        assertNull(shoppingListMapper.selectByIdAndUserId(item.getId(), user.getId()));
        assertTrue(shoppingListMapper.selectAllByUserId(user.getId()).isEmpty());
    }

    @Test
    void mealHistoryMapperRoundTripsJsonSnapshotsAndOrdersByConfirmedTimeAndId() {
        User user = insertTestUser();
        LocalDateTime confirmedTime = LocalDateTime.now().withNano(0);

        MealHistory firstHistory = newMealHistory(user.getId(), "High protein dinner", confirmedTime);
        assertEquals(1, mealHistoryMapper.insert(firstHistory));
        assertNotNull(firstHistory.getId());

        MealHistory selected = mealHistoryMapper.selectByIdAndUserId(firstHistory.getId(), user.getId());
        assertMealHistorySnapshot(selected, "High protein dinner");

        MealHistory secondHistory = newMealHistory(user.getId(), "Vegetarian dinner", confirmedTime);
        assertEquals(1, mealHistoryMapper.insert(secondHistory));
        assertNotNull(secondHistory.getId());

        List<MealHistory> histories = mealHistoryMapper.selectAllByUserId(user.getId());
        assertTrue(histories.size() >= 2);
        assertEquals(secondHistory.getId(), histories.get(0).getId());
        assertEquals(firstHistory.getId(), histories.get(1).getId());
        assertFalse(histories.get(0).getId() < histories.get(1).getId());
    }

    private User insertTestUser() {
        User user = newTestUser();
        assertEquals(1, userMapper.insert(user));
        assertNotNull(user.getId());
        return user;
    }

    private User newTestUser() {
        String uniqueValue = UUID.randomUUID().toString();
        User user = new User();
        user.setUsername("test-user-" + uniqueValue.substring(0, 8));
        user.setEmail("test-" + uniqueValue + "@example.com");
        user.setPassword("test-password-hash");
        user.setStatus(1);
        return user;
    }

    private String uniqueName(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private MealHistory newMealHistory(Long userId, String userRequest, LocalDateTime confirmedTime) {
        MealHistory mealHistory = new MealHistory();
        mealHistory.setUserId(userId);
        mealHistory.setUserRequest(userRequest);
        mealHistory.setRecipes(List.of(newRecipeSnapshot()));
        mealHistory.setShoppingListSnapshot(List.of(newShoppingListSnapshotItem()));
        mealHistory.setConfirmedTime(confirmedTime);
        return mealHistory;
    }

    private RecipeSnapshot newRecipeSnapshot() {
        RecipeIngredientSnapshot ingredient = new RecipeIngredientSnapshot();
        ingredient.setName("Chicken breast");
        ingredient.setQuantity(new BigDecimal("0.50"));
        ingredient.setUnit("kg");

        NutritionSnapshot protein = new NutritionSnapshot();
        protein.setAmount(new BigDecimal("42"));
        protein.setUnit("g");
        protein.setDailyValuePercent(new BigDecimal("84"));

        RecipeSnapshot recipe = new RecipeSnapshot();
        recipe.setId(1001L);
        recipe.setTitle("Test Lemon Chicken");
        recipe.setIngredients(List.of(ingredient));
        recipe.setSteps(List.of("Season chicken.", "Bake until cooked."));
        recipe.setNutrition(Map.of("protein", protein));
        return recipe;
    }

    private ShoppingListSnapshotItem newShoppingListSnapshotItem() {
        ShoppingListSnapshotItem item = new ShoppingListSnapshotItem();
        item.setId("test-shopping-item");
        item.setName("Chicken breast");
        item.setRequiredQuantity(new BigDecimal("0.50"));
        item.setAvailableFromInventory(new BigDecimal("0.10"));
        item.setQuantityToBuy(new BigDecimal("0.40"));
        item.setUnit("kg");
        return item;
    }

    private void assertUserMapping(User expected, User actual, int expectedStatus) {
        assertNotNull(actual);
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getUsername(), actual.getUsername());
        assertEquals(expected.getEmail(), actual.getEmail());
        assertEquals(expectedStatus, actual.getStatus());
        assertNotNull(actual.getCreateTime());
        assertNotNull(actual.getUpdateTime());
    }

    private void assertInventory(Inventory expected, Inventory actual, String expectedQuantity) {
        assertNotNull(actual);
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getUserId(), actual.getUserId());
        assertEquals(expected.getFoodName(), actual.getFoodName());
        assertBigDecimalEquals(expectedQuantity, actual.getQuantity());
        assertEquals(expected.getUnit(), actual.getUnit());
        assertEquals(expected.getAddedDate(), actual.getAddedDate());
        assertEquals(expected.getReminderDays(), actual.getReminderDays());
        assertNotNull(actual.getCreateTime());
        assertNotNull(actual.getUpdateTime());
    }

    private void assertShoppingList(
            ShoppingList expected,
            ShoppingList actual,
            String expectedRequiredQuantity,
            String expectedAvailableQuantity,
            String expectedQuantityToBuy) {
        assertNotNull(actual);
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getUserId(), actual.getUserId());
        assertEquals(expected.getFoodName(), actual.getFoodName());
        assertBigDecimalEquals(expectedRequiredQuantity, actual.getRequiredQuantity());
        assertBigDecimalEquals(expectedAvailableQuantity, actual.getAvailableQuantity());
        assertBigDecimalEquals(expectedQuantityToBuy, actual.getQuantityToBuy());
        assertEquals(expected.getUnit(), actual.getUnit());
        assertNotNull(actual.getCreateTime());
        assertNotNull(actual.getUpdateTime());
    }

    private void assertMealHistorySnapshot(MealHistory selected, String expectedUserRequest) {
        assertNotNull(selected);
        assertEquals(expectedUserRequest, selected.getUserRequest());
        assertNotNull(selected.getConfirmedTime());
        assertNotNull(selected.getCreateTime());

        assertEquals(1, selected.getRecipes().size());
        RecipeSnapshot recipe = selected.getRecipes().get(0);
        assertEquals(1001L, recipe.getId());
        assertEquals("Test Lemon Chicken", recipe.getTitle());
        assertEquals(List.of("Season chicken.", "Bake until cooked."), recipe.getSteps());
        assertEquals(1, recipe.getIngredients().size());
        assertEquals("Chicken breast", recipe.getIngredients().get(0).getName());
        assertBigDecimalEquals("0.50", recipe.getIngredients().get(0).getQuantity());
        assertBigDecimalEquals("42", recipe.getNutrition().get("protein").getAmount());

        assertEquals(1, selected.getShoppingListSnapshot().size());
        ShoppingListSnapshotItem shoppingItem = selected.getShoppingListSnapshot().get(0);
        assertEquals("test-shopping-item", shoppingItem.getId());
        assertEquals("Chicken breast", shoppingItem.getName());
        assertBigDecimalEquals("0.50", shoppingItem.getRequiredQuantity());
        assertBigDecimalEquals("0.10", shoppingItem.getAvailableFromInventory());
        assertBigDecimalEquals("0.40", shoppingItem.getQuantityToBuy());
        assertEquals("kg", shoppingItem.getUnit());
    }

    private void assertBigDecimalEquals(String expected, BigDecimal actual) {
        assertNotNull(actual);
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
