package com.syde.mealplanner.controller;

import com.jayway.jsonpath.JsonPath;
import com.syde.mealplanner.entity.Inventory;
import com.syde.mealplanner.entity.ShoppingList;
import com.syde.mealplanner.mapper.InventoryMapper;
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
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ShoppingListControllerIntegrationTests {

    private static final String RAW_PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ShoppingListMapper shoppingListMapper;

    @Autowired
    private InventoryMapper inventoryMapper;

    @Test
    void unauthenticatedShoppingListRequestsReturn401() throws Exception {
        mockMvc.perform(get("/api/shopping-list"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(delete("/api/shopping-list/{id}", 1L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(post("/api/shopping-list/place-order"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void authenticatedUserSeesOnlyOwnShoppingListWithoutUserId() throws Exception {
        TestUser user = registerAndLogin("shopping-list-user-" + UUID.randomUUID());
        TestUser otherUser = registerAndLogin("shopping-list-other-" + UUID.randomUUID());
        TestUser emptyUser = registerAndLogin("shopping-list-empty-" + UUID.randomUUID());

        ShoppingList ownedItem = insertShoppingList(user.id(), "Chicken", "2.00", "0.50", "1.50", "kg");
        ShoppingList otherItem = insertShoppingList(otherUser.id(), "Private Rice", "3.00", "1.00", "2.00", "kg");

        mockMvc.perform(get("/api/shopping-list")
                        .header("Authorization", bearer(user.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(ownedItem.getId().longValue()))
                .andExpect(jsonPath("$.data[0].foodName").value("Chicken"))
                .andExpect(jsonPath("$.data[0].requiredQuantity").value(2.00))
                .andExpect(jsonPath("$.data[0].availableQuantity").value(0.50))
                .andExpect(jsonPath("$.data[0].quantityToBuy").value(1.50))
                .andExpect(jsonPath("$.data[0].unit").value("kg"))
                .andExpect(jsonPath("$.data[0].createTime").exists())
                .andExpect(jsonPath("$.data[0].updateTime").exists())
                .andExpect(jsonPath("$.data[0].userId").doesNotExist())
                .andExpect(jsonPath("$.data[*].id", not(hasItem(otherItem.getId().intValue()))));

        mockMvc.perform(get("/api/shopping-list")
                        .header("Authorization", bearer(emptyUser.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void deleteRemovesOnlyOwnedShoppingListItem() throws Exception {
        TestUser owner = registerAndLogin("shopping-list-owner-" + UUID.randomUUID());
        TestUser otherUser = registerAndLogin("shopping-list-cross-" + UUID.randomUUID());
        ShoppingList ownedItem = insertShoppingList(owner.id(), "Spinach", "1.00", "0.00", "1.00", "bag");

        mockMvc.perform(delete("/api/shopping-list/{id}", ownedItem.getId())
                        .header("Authorization", bearer(otherUser.token())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Shopping list item not found"));

        assertNotNull(shoppingListMapper.selectByIdAndUserId(ownedItem.getId(), owner.id()));

        mockMvc.perform(delete("/api/shopping-list/{id}", 999999999L)
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

        mockMvc.perform(delete("/api/shopping-list/{id}", ownedItem.getId())
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        assertNull(shoppingListMapper.selectByIdAndUserId(ownedItem.getId(), owner.id()));
    }

    @Test
    void placeOrderCreatesNewInventoryFromQuantityToBuyAndClearsShoppingList() throws Exception {
        TestUser user = registerAndLogin("shopping-list-order-new-" + UUID.randomUUID());
        insertShoppingList(user.id(), "  Tomato  ", "5.00", "1.00", "4.00", " KG ");

        mockMvc.perform(post("/api/shopping-list/place-order")
                        .header("Authorization", bearer(user.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        List<Inventory> inventoryItems = inventoryMapper.selectAllByUserId(user.id());
        assertEquals(1, inventoryItems.size());
        Inventory inventory = inventoryItems.get(0);
        assertEquals("Tomato", inventory.getFoodName());
        assertBigDecimalEquals("4.00", inventory.getQuantity());
        assertEquals("KG", inventory.getUnit());
        assertEquals(LocalDate.now(), inventory.getAddedDate());
        assertEquals(0, inventory.getReminderDays());
        assertTrue(shoppingListMapper.selectAllByUserId(user.id()).isEmpty());
    }

    @Test
    void placeOrderMergesByNormalizedFoodNameAndUnitOnly() throws Exception {
        TestUser user = registerAndLogin("shopping-list-order-merge-" + UUID.randomUUID());
        TestUser otherUser = registerAndLogin("shopping-list-order-other-" + UUID.randomUUID());

        Inventory target = insertInventory(user.id(), "Tomato", "1.50", "kg", LocalDate.now().minusDays(3), 7);
        Inventory duplicate = insertInventory(user.id(), " tomato ", "8.00", "KG", LocalDate.now().minusDays(4), 2);
        Inventory differentUnit = insertInventory(user.id(), "Tomato", "10.00", "g", LocalDate.now().minusDays(5), 4);
        Inventory differentFood = insertInventory(user.id(), "Potato", "6.00", "kg", LocalDate.now().minusDays(6), 5);
        Inventory otherUsersInventory = insertInventory(otherUser.id(), "Tomato", "20.00", "kg", LocalDate.now().minusDays(7), 9);

        insertShoppingList(user.id(), " tomato ", "5.00", "1.00", "2.25", " KG ");

        mockMvc.perform(post("/api/shopping-list/place-order")
                        .header("Authorization", bearer(user.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Inventory updatedTarget = inventoryMapper.selectByIdAndUserId(target.getId(), user.id());
        assertBigDecimalEquals("3.75", updatedTarget.getQuantity());
        assertEquals("Tomato", updatedTarget.getFoodName());
        assertEquals("kg", updatedTarget.getUnit());
        assertEquals(7, updatedTarget.getReminderDays());
        assertEquals(LocalDate.now(), updatedTarget.getAddedDate());

        assertBigDecimalEquals("8.00", inventoryMapper.selectByIdAndUserId(duplicate.getId(), user.id()).getQuantity());
        assertBigDecimalEquals("10.00", inventoryMapper.selectByIdAndUserId(differentUnit.getId(), user.id()).getQuantity());
        assertBigDecimalEquals("6.00", inventoryMapper.selectByIdAndUserId(differentFood.getId(), user.id()).getQuantity());
        assertBigDecimalEquals("20.00", inventoryMapper.selectByIdAndUserId(otherUsersInventory.getId(), otherUser.id()).getQuantity());
        assertEquals(4, inventoryMapper.selectAllByUserId(user.id()).size());
    }

    @Test
    void emptyAndRepeatedPlaceOrderReturn400WithoutIncreasingInventoryAgain() throws Exception {
        TestUser user = registerAndLogin("shopping-list-order-repeat-" + UUID.randomUUID());
        insertShoppingList(user.id(), "Rice", "3.00", "0.00", "3.00", "kg");

        mockMvc.perform(post("/api/shopping-list/place-order")
                        .header("Authorization", bearer(user.token())))
                .andExpect(status().isOk());

        Inventory inventory = inventoryMapper.selectAllByUserId(user.id()).get(0);
        assertBigDecimalEquals("3.00", inventory.getQuantity());

        mockMvc.perform(post("/api/shopping-list/place-order")
                        .header("Authorization", bearer(user.token())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Shopping list is empty"));

        assertBigDecimalEquals("3.00", inventoryMapper.selectByIdAndUserId(inventory.getId(), user.id()).getQuantity());
    }

    @Test
    void placeOrderProcessesOnlyAuthenticatedUsersShoppingListAndInventory() throws Exception {
        TestUser user = registerAndLogin("shopping-list-isolation-user-" + UUID.randomUUID());
        TestUser otherUser = registerAndLogin("shopping-list-isolation-other-" + UUID.randomUUID());

        insertShoppingList(user.id(), "Milk", "2.00", "0.00", "2.00", "L");
        ShoppingList otherItem = insertShoppingList(otherUser.id(), "Milk", "9.00", "0.00", "9.00", "L");
        Inventory otherInventory = insertInventory(otherUser.id(), "Milk", "1.00", "L", LocalDate.now().minusDays(2), 3);

        mockMvc.perform(post("/api/shopping-list/place-order")
                        .header("Authorization", bearer(user.token())))
                .andExpect(status().isOk());

        assertTrue(shoppingListMapper.selectAllByUserId(user.id()).isEmpty());
        assertNotNull(shoppingListMapper.selectByIdAndUserId(otherItem.getId(), otherUser.id()));
        assertBigDecimalEquals("1.00", inventoryMapper.selectByIdAndUserId(otherInventory.getId(), otherUser.id()).getQuantity());
    }

    @Test
    void placeOrderRollsBackInventoryChangesWhenLaterItemFailsValidation() throws Exception {
        TestUser user = registerAndLogin("shopping-list-rollback-" + UUID.randomUUID());
        ShoppingList validItem = insertShoppingList(user.id(), "Beans", "2.00", "0.00", "2.00", "can");
        ShoppingList invalidItem = insertShoppingList(user.id(), "Peas", "1.00", "1.00", "0.00", "bag");

        mockMvc.perform(post("/api/shopping-list/place-order")
                        .header("Authorization", bearer(user.token())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Invalid shopping list quantity"));

        assertTrue(inventoryMapper.selectAllByUserId(user.id()).isEmpty());
        assertNotNull(shoppingListMapper.selectByIdAndUserId(validItem.getId(), user.id()));
        assertNotNull(shoppingListMapper.selectByIdAndUserId(invalidItem.getId(), user.id()));
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

    private Inventory insertInventory(
            Long userId,
            String foodName,
            String quantity,
            String unit,
            LocalDate addedDate,
            int reminderDays) {
        Inventory inventory = new Inventory();
        inventory.setUserId(userId);
        inventory.setFoodName(foodName);
        inventory.setQuantity(new BigDecimal(quantity));
        inventory.setUnit(unit);
        inventory.setAddedDate(addedDate);
        inventory.setReminderDays(reminderDays);
        assertEquals(1, inventoryMapper.insert(inventory));
        assertNotNull(inventory.getId());
        return inventory;
    }

    private TestUser registerAndLogin(String username) throws Exception {
        String safeUsername = username.length() > 40 ? username.substring(0, 40) : username;
        String email = "shopping-list-test-" + UUID.randomUUID() + "@example.com";

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

    private void assertBigDecimalEquals(String expected, BigDecimal actual) {
        assertNotNull(actual);
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record TestUser(String token, Long id) {
    }
}
