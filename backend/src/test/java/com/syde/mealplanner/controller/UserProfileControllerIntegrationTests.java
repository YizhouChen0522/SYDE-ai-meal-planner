package com.syde.mealplanner.controller;

import com.jayway.jsonpath.JsonPath;
import com.syde.mealplanner.entity.UserProfile;
import com.syde.mealplanner.mapper.UserProfileMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserProfileControllerIntegrationTests {

    private static final String RAW_PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserProfileMapper userProfileMapper;

    @Test
    void unauthenticatedProfileRequestsReturn401() throws Exception {
        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(put("/api/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void newlyRegisteredUserCanReadEmptyProfileWithoutInternalFields() throws Exception {
        TestUser user = registerAndLogin("profile-empty-" + UUID.randomUUID());

        mockMvc.perform(get("/api/profile")
                        .header("Authorization", bearer(user.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.likedFoods", hasSize(0)))
                .andExpect(jsonPath("$.data.dislikedFoods", hasSize(0)))
                .andExpect(jsonPath("$.data.allergies", hasSize(0)))
                .andExpect(jsonPath("$.data.id").doesNotExist())
                .andExpect(jsonPath("$.data.userId").doesNotExist())
                .andExpect(jsonPath("$.data.createTime").doesNotExist())
                .andExpect(jsonPath("$.data.updateTime").doesNotExist());
    }

    @Test
    void updateProfilePersistsAllListsAndGetReturnsUpdatedValues() throws Exception {
        TestUser user = registerAndLogin("profile-update-" + UUID.randomUUID());

        mockMvc.perform(put("/api/profile")
                        .header("Authorization", bearer(user.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "likedFoods": ["Chicken", "Pasta"],
                                  "dislikedFoods": ["Celery"],
                                  "allergies": ["Peanut", "Shellfish"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.likedFoods[0]").value("Chicken"))
                .andExpect(jsonPath("$.data.likedFoods[1]").value("Pasta"))
                .andExpect(jsonPath("$.data.dislikedFoods[0]").value("Celery"))
                .andExpect(jsonPath("$.data.allergies[0]").value("Peanut"))
                .andExpect(jsonPath("$.data.allergies[1]").value("Shellfish"));

        UserProfile savedProfile = userProfileMapper.selectByUserId(user.id());
        assertEquals(List.of("Chicken", "Pasta"), savedProfile.getLikedFoods());
        assertEquals(List.of("Celery"), savedProfile.getDislikedFoods());
        assertEquals(List.of("Peanut", "Shellfish"), savedProfile.getAllergies());

        mockMvc.perform(get("/api/profile")
                        .header("Authorization", bearer(user.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likedFoods[0]").value("Chicken"))
                .andExpect(jsonPath("$.data.likedFoods[1]").value("Pasta"))
                .andExpect(jsonPath("$.data.dislikedFoods[0]").value("Celery"))
                .andExpect(jsonPath("$.data.allergies[0]").value("Peanut"))
                .andExpect(jsonPath("$.data.allergies[1]").value("Shellfish"));
    }

    @Test
    void updateUsesAuthenticatedUserAndDoesNotRequireOrHonorUserId() throws Exception {
        TestUser user = registerAndLogin("profile-owner-" + UUID.randomUUID());
        TestUser otherUser = registerAndLogin("profile-other-" + UUID.randomUUID());
        updateProfileRow(otherUser.id(), List.of("Private Rice"), List.of("Private Celery"), List.of("Private Peanut"));

        mockMvc.perform(put("/api/profile")
                        .header("Authorization", bearer(user.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "likedFoods": ["Salmon"],
                                  "dislikedFoods": [],
                                  "allergies": []
                                }
                                """.formatted(otherUser.id())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likedFoods[0]").value("Salmon"))
                .andExpect(jsonPath("$.data.userId").doesNotExist());

        assertEquals(List.of("Salmon"), userProfileMapper.selectByUserId(user.id()).getLikedFoods());
        assertEquals(List.of("Private Rice"), userProfileMapper.selectByUserId(otherUser.id()).getLikedFoods());

        mockMvc.perform(get("/api/profile")
                        .header("Authorization", bearer(otherUser.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likedFoods[0]").value("Private Rice"));
    }

    @Test
    void nullMissingAndEmptyListsNormalizeToEmptyLists() throws Exception {
        TestUser user = registerAndLogin("profile-null-" + UUID.randomUUID());
        updateProfileRow(user.id(), List.of("Chicken"), List.of("Celery"), List.of("Peanut"));

        mockMvc.perform(put("/api/profile")
                        .header("Authorization", bearer(user.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "likedFoods": null,
                                  "dislikedFoods": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likedFoods", hasSize(0)))
                .andExpect(jsonPath("$.data.dislikedFoods", hasSize(0)))
                .andExpect(jsonPath("$.data.allergies", hasSize(0)));

        UserProfile savedProfile = userProfileMapper.selectByUserId(user.id());
        assertEquals(List.of(), savedProfile.getLikedFoods());
        assertEquals(List.of(), savedProfile.getDislikedFoods());
        assertEquals(List.of(), savedProfile.getAllergies());
    }

    @Test
    void profileItemsAreTrimmedAndDuplicatesAreRemovedPreservingOrder() throws Exception {
        TestUser user = registerAndLogin("profile-normalize-" + UUID.randomUUID());

        mockMvc.perform(put("/api/profile")
                        .header("Authorization", bearer(user.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "likedFoods": [" Chicken ", "Chicken", " salmon ", "SALMON", "Pasta"],
                                  "dislikedFoods": [" celery ", "Celery"],
                                  "allergies": [" Peanut ", "peanut", "Shellfish"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likedFoods", hasSize(3)))
                .andExpect(jsonPath("$.data.likedFoods[0]").value("Chicken"))
                .andExpect(jsonPath("$.data.likedFoods[1]").value("salmon"))
                .andExpect(jsonPath("$.data.likedFoods[2]").value("Pasta"))
                .andExpect(jsonPath("$.data.dislikedFoods", hasSize(1)))
                .andExpect(jsonPath("$.data.dislikedFoods[0]").value("celery"))
                .andExpect(jsonPath("$.data.allergies", hasSize(2)))
                .andExpect(jsonPath("$.data.allergies[0]").value("Peanut"))
                .andExpect(jsonPath("$.data.allergies[1]").value("Shellfish"));
    }

    @Test
    void blankAndOverlengthItemsAreRejected() throws Exception {
        TestUser user = registerAndLogin("profile-invalid-" + UUID.randomUUID());

        mockMvc.perform(put("/api/profile")
                        .header("Authorization", bearer(user.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "likedFoods": ["Chicken", "   "],
                                  "dislikedFoods": [],
                                  "allergies": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Profile item must not be blank"));

        String overlengthItem = "x".repeat(101);
        mockMvc.perform(put("/api/profile")
                        .header("Authorization", bearer(user.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "likedFoods": ["Chicken"],
                                  "dislikedFoods": ["%s"],
                                  "allergies": []
                                }
                                """.formatted(overlengthItem)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Profile item must be at most 100 characters"));
    }

    private void updateProfileRow(Long userId, List<String> likedFoods, List<String> dislikedFoods, List<String> allergies) {
        UserProfile profile = userProfileMapper.selectByUserId(userId);
        assertNotNull(profile);
        profile.setLikedFoods(likedFoods);
        profile.setDislikedFoods(dislikedFoods);
        profile.setAllergies(allergies);
        assertEquals(1, userProfileMapper.updateByUserId(profile));
    }

    private TestUser registerAndLogin(String username) throws Exception {
        String safeUsername = username.length() > 40 ? username.substring(0, 40) : username;
        String email = "profile-test-" + UUID.randomUUID() + "@example.com";

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
