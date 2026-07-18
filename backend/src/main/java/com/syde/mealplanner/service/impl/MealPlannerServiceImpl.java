package com.syde.mealplanner.service.impl;

import com.syde.mealplanner.entity.Inventory;
import com.syde.mealplanner.entity.MealHistory;
import com.syde.mealplanner.entity.MealPlanDraft;
import com.syde.mealplanner.entity.ShoppingList;
import com.syde.mealplanner.entity.json.RecipeIngredientSnapshot;
import com.syde.mealplanner.entity.json.RecipeSnapshot;
import com.syde.mealplanner.entity.json.ShoppingListSnapshotItem;
import com.syde.mealplanner.exception.BusinessException;
import com.syde.mealplanner.mapper.InventoryMapper;
import com.syde.mealplanner.mapper.MealHistoryMapper;
import com.syde.mealplanner.mapper.MealPlanDraftMapper;
import com.syde.mealplanner.mapper.ShoppingListMapper;
import com.syde.mealplanner.provider.MealGenerationProvider;
import com.syde.mealplanner.security.AuthenticatedUser;
import com.syde.mealplanner.service.MealPlannerService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class MealPlannerServiceImpl implements MealPlannerService {

    private final MealGenerationProvider mealGenerationProvider;
    private final MealPlanDraftMapper mealPlanDraftMapper;
    private final InventoryMapper inventoryMapper;
    private final ShoppingListMapper shoppingListMapper;
    private final MealHistoryMapper mealHistoryMapper;

    public MealPlannerServiceImpl(
            MealGenerationProvider mealGenerationProvider,
            MealPlanDraftMapper mealPlanDraftMapper,
            InventoryMapper inventoryMapper,
            ShoppingListMapper shoppingListMapper,
            MealHistoryMapper mealHistoryMapper) {
        this.mealGenerationProvider = mealGenerationProvider;
        this.mealPlanDraftMapper = mealPlanDraftMapper;
        this.inventoryMapper = inventoryMapper;
        this.shoppingListMapper = shoppingListMapper;
        this.mealHistoryMapper = mealHistoryMapper;
    }

    @Override
    @Transactional
    public MealPlanDraft generateDraft(String userRequest) {
        Long userId = getCurrentUserId();
        String cleanUserRequest = requireUserRequest(userRequest);
        List<RecipeSnapshot> recipes = mealGenerationProvider.generate(cleanUserRequest);

        MealPlanDraft existingDraft = mealPlanDraftMapper.selectByUserId(userId);
        if (existingDraft == null) {
            MealPlanDraft newDraft = new MealPlanDraft();
            newDraft.setUserId(userId);
            newDraft.setUserRequest(cleanUserRequest);
            newDraft.setRecipes(recipes);

            if (mealPlanDraftMapper.insert(newDraft) != 1 || newDraft.getId() == null) {
                throw new BusinessException(500, "Could not create meal plan draft");
            }

            return mealPlanDraftMapper.selectByIdAndUserId(newDraft.getId(), userId);
        }

        existingDraft.setUserRequest(cleanUserRequest);
        existingDraft.setRecipes(recipes);

        if (mealPlanDraftMapper.updateByUserId(existingDraft) != 1) {
            throw new BusinessException(500, "Could not update meal plan draft");
        }

        return mealPlanDraftMapper.selectByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public MealPlanDraft getCurrentDraft() {
        return mealPlanDraftMapper.selectByUserId(getCurrentUserId());
    }

    @Override
    @Transactional
    public void deleteCurrentDraft() {
        Long userId = getCurrentUserId();
        int deletedRows = mealPlanDraftMapper.deleteByUserId(userId);
        if (deletedRows != 1) {
            throw new BusinessException(404, "Meal plan draft not found");
        }
    }

    @Override
    @Transactional
    public MealPlanDraft replaceRecipe(Long recipeId) {
        Long userId = getCurrentUserId();
        MealPlanDraft draft = mealPlanDraftMapper.selectByUserId(userId);
        if (draft == null) {
            throw new BusinessException(404, "Meal plan draft not found");
        }

        List<RecipeSnapshot> currentRecipes = draft.getRecipes();
        if (currentRecipes == null || currentRecipes.isEmpty()) {
            throw new BusinessException(404, "Recipe not found in meal plan draft");
        }

        int replaceIndex = findRecipeIndex(currentRecipes, recipeId);
        if (replaceIndex < 0) {
            throw new BusinessException(404, "Recipe not found in meal plan draft");
        }

        List<RecipeSnapshot> updatedRecipes = new ArrayList<>(currentRecipes);
        RecipeSnapshot recipeToReplace = updatedRecipes.get(replaceIndex);
        RecipeSnapshot replacement = mealGenerationProvider.generateReplacement(
                draft.getUserRequest(),
                recipeToReplace,
                List.copyOf(currentRecipes));

        validateReplacementRecipe(replacement, recipeToReplace, updatedRecipes);
        updatedRecipes.set(replaceIndex, replacement);

        draft.setRecipes(updatedRecipes);
        if (mealPlanDraftMapper.updateByUserId(draft) != 1) {
            throw new BusinessException(500, "Could not update meal plan draft");
        }

        return mealPlanDraftMapper.selectByUserId(userId);
    }

    @Override
    @Transactional
    public MealPlanDraft addRecipe() {
        Long userId = getCurrentUserId();
        MealPlanDraft draft = mealPlanDraftMapper.selectByUserId(userId);
        if (draft == null) {
            throw new BusinessException(404, "Meal plan draft not found");
        }

        List<RecipeSnapshot> currentRecipes = draft.getRecipes() == null
                ? List.of()
                : draft.getRecipes();
        List<RecipeSnapshot> updatedRecipes = new ArrayList<>(currentRecipes);
        RecipeSnapshot additionalRecipe = mealGenerationProvider.generateAdditionalRecipe(
                draft.getUserRequest(),
                List.copyOf(currentRecipes));

        validateAdditionalRecipe(additionalRecipe, currentRecipes);
        updatedRecipes.add(additionalRecipe);

        draft.setRecipes(updatedRecipes);
        if (mealPlanDraftMapper.updateByUserId(draft) != 1) {
            throw new BusinessException(500, "Could not update meal plan draft");
        }

        return mealPlanDraftMapper.selectByUserId(userId);
    }

    @Override
    @Transactional
    public MealPlanDraft deleteRecipe(Long recipeId) {
        Long userId = getCurrentUserId();
        MealPlanDraft draft = mealPlanDraftMapper.selectByUserId(userId);
        if (draft == null) {
            throw new BusinessException(404, "Meal plan draft not found");
        }

        List<RecipeSnapshot> currentRecipes = draft.getRecipes();
        if (currentRecipes == null || currentRecipes.isEmpty()) {
            throw new BusinessException(404, "Recipe not found in meal plan draft");
        }

        int deleteIndex = findRecipeIndex(currentRecipes, recipeId);
        if (deleteIndex < 0) {
            throw new BusinessException(404, "Recipe not found in meal plan draft");
        }

        List<RecipeSnapshot> updatedRecipes = new ArrayList<>(currentRecipes);
        updatedRecipes.remove(deleteIndex);

        draft.setRecipes(updatedRecipes);
        if (mealPlanDraftMapper.updateByUserId(draft) != 1) {
            throw new BusinessException(500, "Could not update meal plan draft");
        }

        return mealPlanDraftMapper.selectByUserId(userId);
    }

    @Override
    @Transactional
    public MealHistory confirmCurrentDraft() {
        Long userId = getCurrentUserId();
        MealPlanDraft draft = mealPlanDraftMapper.selectByUserId(userId);
        if (draft == null) {
            throw new BusinessException(404, "Meal plan draft not found");
        }

        List<RecipeSnapshot> recipeSnapshot = draft.getRecipes() == null
                ? List.of()
                : List.copyOf(draft.getRecipes());
        List<PurchaseRequirement> purchaseRequirements = calculatePurchaseRequirements(userId, recipeSnapshot);

        mergeShoppingListRequirements(userId, purchaseRequirements);

        MealHistory mealHistory = new MealHistory();
        mealHistory.setUserId(userId);
        mealHistory.setUserRequest(draft.getUserRequest());
        mealHistory.setRecipes(recipeSnapshot);
        mealHistory.setShoppingListSnapshot(toShoppingListSnapshot(purchaseRequirements));
        mealHistory.setConfirmedTime(LocalDateTime.now());

        if (mealHistoryMapper.insert(mealHistory) != 1 || mealHistory.getId() == null) {
            throw new BusinessException(500, "Could not create meal history");
        }

        if (mealPlanDraftMapper.deleteByUserId(userId) != 1) {
            throw new BusinessException(500, "Could not delete meal plan draft");
        }

        return mealHistoryMapper.selectByIdAndUserId(mealHistory.getId(), userId);
    }

    private int findRecipeIndex(List<RecipeSnapshot> recipes, Long recipeId) {
        for (int index = 0; index < recipes.size(); index++) {
            if (Objects.equals(recipes.get(index).getId(), recipeId)) {
                return index;
            }
        }
        return -1;
    }

    private void validateReplacementRecipe(
            RecipeSnapshot replacement,
            RecipeSnapshot recipeToReplace,
            List<RecipeSnapshot> currentRecipes) {
        if (replacement == null || replacement.getId() == null
                || Objects.equals(replacement.getId(), recipeToReplace.getId())) {
            throw new BusinessException(500, "Could not replace recipe");
        }

        boolean idConflict = currentRecipes.stream()
                .filter(recipe -> !Objects.equals(recipe.getId(), recipeToReplace.getId()))
                .map(RecipeSnapshot::getId)
                .anyMatch(replacement.getId()::equals);
        if (idConflict) {
            throw new BusinessException(500, "Could not replace recipe");
        }
    }

    private void validateAdditionalRecipe(RecipeSnapshot additionalRecipe, List<RecipeSnapshot> currentRecipes) {
        if (additionalRecipe == null || additionalRecipe.getId() == null) {
            throw new BusinessException(500, "Could not add recipe");
        }

        boolean idConflict = currentRecipes.stream()
                .map(RecipeSnapshot::getId)
                .anyMatch(additionalRecipe.getId()::equals);
        if (idConflict) {
            throw new BusinessException(500, "Could not add recipe");
        }
    }

    private List<PurchaseRequirement> calculatePurchaseRequirements(Long userId, List<RecipeSnapshot> recipes) {
        Map<IngredientKey, AggregatedIngredient> requiredIngredients = aggregateRequiredIngredients(recipes);
        if (requiredIngredients.isEmpty()) {
            return List.of();
        }

        List<Inventory> inventoryItems = inventoryMapper.selectAllByUserId(userId);
        List<PurchaseRequirement> requirements = new ArrayList<>();

        for (AggregatedIngredient ingredient : requiredIngredients.values()) {
            BigDecimal availableQuantity = inventoryItems.stream()
                    .filter(inventory -> ingredient.key().equals(IngredientKey.from(inventory.getFoodName(), inventory.getUnit())))
                    .map(Inventory::getQuantity)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal quantityToBuy = ingredient.requiredQuantity().subtract(availableQuantity).max(BigDecimal.ZERO);
            if (quantityToBuy.compareTo(BigDecimal.ZERO) > 0) {
                requirements.add(new PurchaseRequirement(
                        ingredient.displayName(),
                        ingredient.unit(),
                        ingredient.requiredQuantity(),
                        availableQuantity,
                        quantityToBuy,
                        ingredient.key()));
            }
        }

        return requirements;
    }

    private Map<IngredientKey, AggregatedIngredient> aggregateRequiredIngredients(List<RecipeSnapshot> recipes) {
        Map<IngredientKey, AggregatedIngredient> ingredients = new LinkedHashMap<>();
        for (RecipeSnapshot recipe : recipes) {
            if (recipe.getIngredients() == null) {
                continue;
            }
            for (RecipeIngredientSnapshot ingredient : recipe.getIngredients()) {
                if (ingredient.getQuantity() == null || ingredient.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                String displayName = trimRequired(ingredient.getName(), "Invalid recipe ingredient");
                String unit = trimRequired(ingredient.getUnit(), "Invalid recipe ingredient unit");
                IngredientKey key = IngredientKey.from(displayName, unit);
                AggregatedIngredient current = ingredients.get(key);
                if (current == null) {
                    ingredients.put(key, new AggregatedIngredient(
                            key,
                            displayName,
                            unit,
                            ingredient.getQuantity()));
                } else {
                    ingredients.put(key, new AggregatedIngredient(
                            key,
                            current.displayName(),
                            current.unit(),
                            current.requiredQuantity().add(ingredient.getQuantity())));
                }
            }
        }
        return ingredients;
    }

    private void mergeShoppingListRequirements(Long userId, List<PurchaseRequirement> purchaseRequirements) {
        if (purchaseRequirements.isEmpty()) {
            return;
        }

        List<ShoppingList> shoppingListItems = new ArrayList<>(shoppingListMapper.selectAllByUserId(userId));
        for (PurchaseRequirement requirement : purchaseRequirements) {
            Optional<ShoppingList> existingItem = shoppingListItems.stream()
                    .filter(item -> requirement.key().equals(IngredientKey.from(item.getFoodName(), item.getUnit())))
                    .findFirst();

            if (existingItem.isPresent()) {
                ShoppingList item = existingItem.get();
                BigDecimal updatedRequiredQuantity = nullSafe(item.getRequiredQuantity()).add(requirement.requiredQuantity());
                BigDecimal updatedAvailableQuantity = nullSafe(item.getAvailableQuantity()).add(requirement.availableQuantity());
                BigDecimal updatedQuantityToBuy = nullSafe(item.getQuantityToBuy()).add(requirement.quantityToBuy());
                if (shoppingListMapper.updateQuantitiesByIdAndUserId(
                        item.getId(),
                        userId,
                        updatedRequiredQuantity,
                        updatedAvailableQuantity,
                        updatedQuantityToBuy) != 1) {
                    throw new BusinessException(500, "Could not update shopping list");
                }
                item.setRequiredQuantity(updatedRequiredQuantity);
                item.setAvailableQuantity(updatedAvailableQuantity);
                item.setQuantityToBuy(updatedQuantityToBuy);
            } else {
                ShoppingList item = new ShoppingList();
                item.setUserId(userId);
                item.setFoodName(requirement.displayName());
                item.setRequiredQuantity(requirement.requiredQuantity());
                item.setAvailableQuantity(requirement.availableQuantity());
                item.setQuantityToBuy(requirement.quantityToBuy());
                item.setUnit(requirement.unit());
                if (shoppingListMapper.insert(item) != 1 || item.getId() == null) {
                    throw new BusinessException(500, "Could not create shopping list item");
                }
                shoppingListItems.add(item);
            }
        }
    }

    private List<ShoppingListSnapshotItem> toShoppingListSnapshot(List<PurchaseRequirement> purchaseRequirements) {
        return purchaseRequirements.stream()
                .map(requirement -> {
                    ShoppingListSnapshotItem item = new ShoppingListSnapshotItem();
                    item.setId(requirement.key().name() + "|" + requirement.key().unit());
                    item.setName(requirement.displayName());
                    item.setRequiredQuantity(requirement.requiredQuantity());
                    item.setAvailableFromInventory(requirement.availableQuantity());
                    item.setQuantityToBuy(requirement.quantityToBuy());
                    item.setUnit(requirement.unit());
                    return item;
                })
                .toList();
    }

    private String trimRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(400, message);
        }
        return value.trim();
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)) {
            throw new BusinessException(401, "Authentication is required");
        }
        return authenticatedUser.id();
    }

    private String requireUserRequest(String userRequest) {
        if (userRequest == null || userRequest.trim().isEmpty()) {
            throw new BusinessException(400, "User request is required");
        }
        return userRequest.trim();
    }

    private record IngredientKey(String name, String unit) {

        private static IngredientKey from(String name, String unit) {
            return new IngredientKey(normalize(name), normalize(unit));
        }

        private static String normalize(String value) {
            return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        }
    }

    private record AggregatedIngredient(
            IngredientKey key,
            String displayName,
            String unit,
            BigDecimal requiredQuantity) {
    }

    private record PurchaseRequirement(
            String displayName,
            String unit,
            BigDecimal requiredQuantity,
            BigDecimal availableQuantity,
            BigDecimal quantityToBuy,
            IngredientKey key) {
    }
}
