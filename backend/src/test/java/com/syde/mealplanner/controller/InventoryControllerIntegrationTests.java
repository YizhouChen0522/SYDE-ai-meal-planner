package com.syde.mealplanner.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InventoryControllerIntegrationTests {

    private static final String RAW_PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createListAndGetInventoryForAuthenticatedUser() throws Exception {
        String token = registerAndLogin("inventory-user-" + UUID.randomUUID());

        String responseBody = createInventory(token, "Chicken Breast", "2.50", "kg", "2026-07-17", 3);
        Number inventoryId = JsonPath.read(responseBody, "$.data.id");

        mockMvc.perform(get("/api/inventory")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(inventoryId.longValue()))
                .andExpect(jsonPath("$.data[0].foodName").value("Chicken Breast"))
                .andExpect(jsonPath("$.data[0].quantity").value(2.50))
                .andExpect(jsonPath("$.data[0].unit").value("kg"))
                .andExpect(jsonPath("$.data[0].addedDate").value("2026-07-17"))
                .andExpect(jsonPath("$.data[0].reminderDays").value(3))
                .andExpect(jsonPath("$.data[0].userId").doesNotExist());

        mockMvc.perform(get("/api/inventory/{id}", inventoryId.longValue())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(inventoryId.longValue()))
                .andExpect(jsonPath("$.data.foodName").value("Chicken Breast"))
                .andExpect(jsonPath("$.data.userId").doesNotExist());
    }

    @Test
    void updateInventoryChangesOnlyAllowedFields() throws Exception {
        String token = registerAndLogin("inventory-update-" + UUID.randomUUID());
        String responseBody = createInventory(token, "Brown Rice", "1.25", "kg", "2026-07-17", 2);
        Number inventoryId = JsonPath.read(responseBody, "$.data.id");

        mockMvc.perform(put("/api/inventory/{id}", inventoryId.longValue())
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 4.75,
                                  "addedDate": "2026-07-18",
                                  "reminderDays": 5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(inventoryId.longValue()))
                .andExpect(jsonPath("$.data.foodName").value("Brown Rice"))
                .andExpect(jsonPath("$.data.unit").value("kg"))
                .andExpect(jsonPath("$.data.quantity").value(4.75))
                .andExpect(jsonPath("$.data.addedDate").value("2026-07-18"))
                .andExpect(jsonPath("$.data.reminderDays").value(5))
                .andExpect(jsonPath("$.data.userId").doesNotExist());
    }

    @Test
    void deleteInventoryRemovesOnlyOwnedItem() throws Exception {
        String token = registerAndLogin("inventory-delete-" + UUID.randomUUID());
        String responseBody = createInventory(token, "Spinach", "1.00", "kg", "2026-07-17", 1);
        Number inventoryId = JsonPath.read(responseBody, "$.data.id");

        mockMvc.perform(delete("/api/inventory/{id}", inventoryId.longValue())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/inventory/{id}", inventoryId.longValue())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void unauthenticatedInventoryRequestsReturn401() throws Exception {
        mockMvc.perform(get("/api/inventory"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "foodName": "Tomato",
                                  "quantity": 1.5,
                                  "unit": "kg",
                                  "addedDate": "2026-07-17",
                                  "reminderDays": 3
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void otherUsersInventoryBehavesAsNotFound() throws Exception {
        String ownerToken = registerAndLogin("inventory-owner-" + UUID.randomUUID());
        String otherToken = registerAndLogin("inventory-other-" + UUID.randomUUID());

        String responseBody = createInventory(ownerToken, "Private Carrot", "2.00", "kg", "2026-07-17", 4);
        Number inventoryId = JsonPath.read(responseBody, "$.data.id");

        mockMvc.perform(get("/api/inventory/{id}", inventoryId.longValue())
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

        mockMvc.perform(get("/api/inventory")
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id", not(hasItem(inventoryId.intValue()))));

        mockMvc.perform(put("/api/inventory/{id}", inventoryId.longValue())
                        .header("Authorization", bearer(otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 9.00,
                                  "addedDate": "2026-07-18",
                                  "reminderDays": 9
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

        mockMvc.perform(delete("/api/inventory/{id}", inventoryId.longValue())
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

        mockMvc.perform(get("/api/inventory/{id}", inventoryId.longValue())
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantity").value(2.00));
    }

    @Test
    void validationFailuresReturn400() throws Exception {
        String token = registerAndLogin("inventory-validation-" + UUID.randomUUID());
        String responseBody = createInventory(token, "Validation Rice", "1.00", "kg", "2026-07-17", 1);
        Number inventoryId = JsonPath.read(responseBody, "$.data.id");

        mockMvc.perform(post("/api/inventory")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "foodName": " ",
                                  "quantity": -1,
                                  "unit": "",
                                  "addedDate": null,
                                  "reminderDays": -1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(put("/api/inventory/{id}", inventoryId.longValue())
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 0,
                                  "addedDate": null,
                                  "reminderDays": -1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    private String createInventory(
            String token,
            String foodName,
            String quantity,
            String unit,
            String addedDate,
            int reminderDays) throws Exception {
        return mockMvc.perform(post("/api/inventory")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "foodName": "%s",
                                  "quantity": %s,
                                  "unit": "%s",
                                  "addedDate": "%s",
                                  "reminderDays": %d
                                }
                                """.formatted(foodName, quantity, unit, addedDate, reminderDays)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.foodName").value(foodName))
                .andExpect(jsonPath("$.data.userId").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private String registerAndLogin(String username) throws Exception {
        String safeUsername = username.length() > 40 ? username.substring(0, 40) : username;
        String email = "inventory-test-" + UUID.randomUUID() + "@example.com";

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

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
