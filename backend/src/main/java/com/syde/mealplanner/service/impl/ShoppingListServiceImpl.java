package com.syde.mealplanner.service.impl;

import com.syde.mealplanner.dto.ShoppingListResponse;
import com.syde.mealplanner.entity.Inventory;
import com.syde.mealplanner.entity.ShoppingList;
import com.syde.mealplanner.exception.BusinessException;
import com.syde.mealplanner.mapper.InventoryMapper;
import com.syde.mealplanner.mapper.ShoppingListMapper;
import com.syde.mealplanner.security.AuthenticatedUser;
import com.syde.mealplanner.service.ShoppingListService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class ShoppingListServiceImpl implements ShoppingListService {

    private final ShoppingListMapper shoppingListMapper;
    private final InventoryMapper inventoryMapper;

    public ShoppingListServiceImpl(ShoppingListMapper shoppingListMapper, InventoryMapper inventoryMapper) {
        this.shoppingListMapper = shoppingListMapper;
        this.inventoryMapper = inventoryMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShoppingListResponse> getAll() {
        Long userId = getCurrentUserId();
        return shoppingListMapper.selectAllByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Long userId = getCurrentUserId();
        int deletedRows = shoppingListMapper.deleteByIdAndUserId(id, userId);
        if (deletedRows != 1) {
            throwNotFound();
        }
    }

    @Override
    @Transactional
    public void placeOrder() {
        Long userId = getCurrentUserId();
        List<ShoppingList> shoppingListItems = shoppingListMapper.selectAllByUserId(userId);
        if (shoppingListItems.isEmpty()) {
            throw new BusinessException(400, "Shopping list is empty");
        }

        List<Inventory> inventoryItems = inventoryMapper.selectAllByUserId(userId);
        LocalDate orderDate = LocalDate.now();

        for (ShoppingList item : shoppingListItems) {
            BigDecimal orderedQuantity = item.getQuantityToBuy();
            if (orderedQuantity == null || orderedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(400, "Invalid shopping list quantity");
            }

            String foodName = trimRequired(item.getFoodName());
            String unit = trimRequired(item.getUnit());

            Optional<Inventory> matchingInventory = inventoryItems.stream()
                    .filter(inventory -> sameIngredient(inventory, foodName, unit))
                    .min(Comparator.comparing(Inventory::getId));

            if (matchingInventory.isPresent()) {
                mergeIntoInventory(userId, matchingInventory.get(), orderedQuantity, orderDate);
            } else {
                inventoryItems.add(createInventory(userId, foodName, orderedQuantity, unit, orderDate));
            }
        }

        int deletedRows = shoppingListMapper.deleteAllByUserId(userId);
        if (deletedRows != shoppingListItems.size()) {
            throw new BusinessException(500, "Could not clear shopping list");
        }
    }

    private Inventory createInventory(
            Long userId,
            String foodName,
            BigDecimal orderedQuantity,
            String unit,
            LocalDate orderDate) {
        Inventory inventory = new Inventory();
        inventory.setUserId(userId);
        inventory.setFoodName(foodName);
        inventory.setQuantity(orderedQuantity);
        inventory.setUnit(unit);
        inventory.setAddedDate(orderDate);
        inventory.setReminderDays(0);

        if (inventoryMapper.insert(inventory) != 1 || inventory.getId() == null) {
            throw new BusinessException(500, "Could not add ordered item to inventory");
        }

        Inventory savedInventory = inventoryMapper.selectByIdAndUserId(inventory.getId(), userId);
        if (savedInventory == null) {
            throw new BusinessException(500, "Could not add ordered item to inventory");
        }
        return savedInventory;
    }

    private void mergeIntoInventory(Long userId, Inventory inventory, BigDecimal orderedQuantity, LocalDate orderDate) {
        BigDecimal existingQuantity = inventory.getQuantity();
        if (existingQuantity == null) {
            throw new BusinessException(500, "Could not update inventory item");
        }

        BigDecimal updatedQuantity = existingQuantity.add(orderedQuantity);
        int updatedRows = inventoryMapper.updateQuantityAndAddedDateByIdAndUserId(
                inventory.getId(),
                userId,
                updatedQuantity,
                orderDate);

        if (updatedRows != 1) {
            throw new BusinessException(500, "Could not update inventory item");
        }

        inventory.setQuantity(updatedQuantity);
        inventory.setAddedDate(orderDate);
    }

    private boolean sameIngredient(Inventory inventory, String foodName, String unit) {
        return normalize(inventory.getFoodName()).equals(normalize(foodName))
                && normalize(inventory.getUnit()).equals(normalize(unit));
    }

    private String trimRequired(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(400, "Invalid shopping list item");
        }
        return value.trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)) {
            throw new BusinessException(401, "Authentication is required");
        }
        return authenticatedUser.id();
    }

    private void throwNotFound() {
        throw new BusinessException(404, "Shopping list item not found");
    }

    private ShoppingListResponse toResponse(ShoppingList shoppingList) {
        return new ShoppingListResponse(
                shoppingList.getId(),
                shoppingList.getFoodName(),
                shoppingList.getRequiredQuantity(),
                shoppingList.getAvailableQuantity(),
                shoppingList.getQuantityToBuy(),
                shoppingList.getUnit(),
                shoppingList.getCreateTime(),
                shoppingList.getUpdateTime());
    }
}
