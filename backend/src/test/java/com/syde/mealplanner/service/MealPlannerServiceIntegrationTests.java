package com.syde.mealplanner.service;

import com.syde.mealplanner.entity.MealPlanDraft;
import com.syde.mealplanner.entity.User;
import com.syde.mealplanner.exception.BusinessException;
import com.syde.mealplanner.mapper.MealPlanDraftMapper;
import com.syde.mealplanner.mapper.UserMapper;
import com.syde.mealplanner.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class MealPlannerServiceIntegrationTests {

    @Autowired
    private MealPlannerService mealPlannerService;

    @Autowired
    private MealPlanDraftMapper mealPlanDraftMapper;

    @Autowired
    private UserMapper userMapper;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void generateDraftCreatesDraftForAuthenticatedUser() {
        User user = insertTestUser();
        authenticate(user);

        MealPlanDraft draft = mealPlannerService.generateDraft("high protein dinners");

        assertNotNull(draft);
        assertNotNull(draft.getId());
        assertEquals(user.getId(), draft.getUserId());
        assertEquals("high protein dinners", draft.getUserRequest());
        assertNotNull(draft.getCreateTime());
        assertNotNull(draft.getUpdateTime());
        assertEquals(5, draft.getRecipes().size());
        assertEquals("Lemon Chicken Pasta", draft.getRecipes().get(0).getTitle());

        MealPlanDraft savedDraft = mealPlanDraftMapper.selectByUserId(user.getId());
        assertNotNull(savedDraft);
        assertEquals(draft.getId(), savedDraft.getId());
        assertEquals("Lemon Chicken Pasta", savedDraft.getRecipes().get(0).getTitle());
    }

    @Test
    void generateDraftReplacesExistingDraftForSameUser() {
        User user = insertTestUser();
        authenticate(user);

        MealPlanDraft firstDraft = mealPlannerService.generateDraft("high protein dinners");
        MealPlanDraft secondDraft = mealPlannerService.generateDraft("vegetarian dinners");

        assertEquals(firstDraft.getId(), secondDraft.getId());
        assertEquals("vegetarian dinners", secondDraft.getUserRequest());
        assertEquals("Chickpea Tomato Pasta", secondDraft.getRecipes().get(0).getTitle());
    }

    @Test
    void getCurrentDraftReturnsOnlyAuthenticatedUsersDraftOrNull() {
        User user = insertTestUser();
        User otherUser = insertTestUser();

        authenticate(otherUser);
        MealPlanDraft otherDraft = mealPlannerService.generateDraft("vegetarian meals");

        authenticate(user);
        assertNull(mealPlannerService.getCurrentDraft());

        MealPlanDraft userDraft = mealPlannerService.generateDraft("high protein meals");
        MealPlanDraft currentDraft = mealPlannerService.getCurrentDraft();

        assertEquals(userDraft.getId(), currentDraft.getId());
        assertNotEquals(otherDraft.getId(), currentDraft.getId());
        assertEquals("Lemon Chicken Pasta", currentDraft.getRecipes().get(0).getTitle());
    }

    @Test
    void deleteCurrentDraftDeletesOnlyAuthenticatedUsersDraft() {
        User user = insertTestUser();
        User otherUser = insertTestUser();

        authenticate(user);
        MealPlanDraft userDraft = mealPlannerService.generateDraft("high protein meals");

        authenticate(otherUser);
        MealPlanDraft otherDraft = mealPlannerService.generateDraft("vegetarian meals");

        authenticate(user);
        mealPlannerService.deleteCurrentDraft();

        assertNull(mealPlanDraftMapper.selectByUserId(user.getId()));
        assertNotNull(mealPlanDraftMapper.selectByIdAndUserId(otherDraft.getId(), otherUser.getId()));
        assertNull(mealPlanDraftMapper.selectByIdAndUserId(userDraft.getId(), user.getId()));
    }

    @Test
    void deleteCurrentDraftThrowsWhenDraftDoesNotExist() {
        User user = insertTestUser();
        authenticate(user);

        BusinessException exception = assertThrows(BusinessException.class, mealPlannerService::deleteCurrentDraft);

        assertEquals(404, exception.getCode());
        assertEquals("Meal plan draft not found", exception.getMessage());
    }

    @Test
    void serviceRequiresAuthenticatedUser() {
        BusinessException generateException = assertThrows(
                BusinessException.class,
                () -> mealPlannerService.generateDraft("high protein meals"));
        assertEquals(401, generateException.getCode());

        BusinessException getException = assertThrows(BusinessException.class, mealPlannerService::getCurrentDraft);
        assertEquals(401, getException.getCode());

        BusinessException deleteException = assertThrows(BusinessException.class, mealPlannerService::deleteCurrentDraft);
        assertEquals(401, deleteException.getCode());
    }

    @Test
    void generateDraftRequiresUserRequest() {
        User user = insertTestUser();
        authenticate(user);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> mealPlannerService.generateDraft(" "));

        assertEquals(400, exception.getCode());
        assertEquals("User request is required", exception.getMessage());
    }

    private User insertTestUser() {
        String uniqueValue = UUID.randomUUID().toString();
        User user = new User();
        user.setUsername("planner-" + uniqueValue.substring(0, 8));
        user.setEmail("planner-" + uniqueValue + "@example.com");
        user.setPassword("test-password-hash");
        user.setStatus(1);
        assertEquals(1, userMapper.insert(user));
        assertNotNull(user.getId());
        return user;
    }

    private void authenticate(User user) {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                AuthenticatedUser.from(user),
                null);
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
