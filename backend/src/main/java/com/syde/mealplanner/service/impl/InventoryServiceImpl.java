package com.syde.mealplanner.service.impl;

import com.syde.mealplanner.dto.InventoryCreateRequest;
import com.syde.mealplanner.dto.InventoryResponse;
import com.syde.mealplanner.dto.InventoryUpdateRequest;
import com.syde.mealplanner.entity.Inventory;
import com.syde.mealplanner.exception.BusinessException;
import com.syde.mealplanner.mapper.InventoryMapper;
import com.syde.mealplanner.security.AuthenticatedUser;
import com.syde.mealplanner.service.InventoryService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventoryServiceImpl implements InventoryService {

    private final InventoryMapper inventoryMapper;

    public InventoryServiceImpl(InventoryMapper inventoryMapper) {
        this.inventoryMapper = inventoryMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryResponse> getAll() {
        Long userId = getCurrentUserId();
        return inventoryMapper.selectAllByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getById(Long id) {
        return toResponse(findOwnedInventoryOrThrow(id));
    }

    @Override
    @Transactional
    public InventoryResponse create(InventoryCreateRequest request) {
        Long userId = getCurrentUserId();

        Inventory inventory = new Inventory();
        inventory.setUserId(userId);
        inventory.setFoodName(request.foodName().trim());
        inventory.setQuantity(request.quantity());
        inventory.setUnit(request.unit().trim());
        inventory.setAddedDate(request.addedDate());
        inventory.setReminderDays(request.reminderDays());

        if (inventoryMapper.insert(inventory) != 1 || inventory.getId() == null) {
            throw new BusinessException(500, "Could not create inventory item");
        }

        return toResponse(inventoryMapper.selectByIdAndUserId(inventory.getId(), userId));
    }

    @Override
    @Transactional
    public InventoryResponse update(Long id, InventoryUpdateRequest request) {
        Long userId = getCurrentUserId();
        findOwnedInventoryOrThrow(id);

        int updatedRows = inventoryMapper.updateDetailsByIdAndUserId(
                id,
                userId,
                request.quantity(),
                request.addedDate(),
                request.reminderDays());

        if (updatedRows != 1) {
            throwNotFound();
        }

        return toResponse(inventoryMapper.selectByIdAndUserId(id, userId));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Long userId = getCurrentUserId();
        int deletedRows = inventoryMapper.deleteByIdAndUserId(id, userId);
        if (deletedRows != 1) {
            throwNotFound();
        }
    }

    private Inventory findOwnedInventoryOrThrow(Long id) {
        Inventory inventory = inventoryMapper.selectByIdAndUserId(id, getCurrentUserId());
        if (inventory == null) {
            throwNotFound();
        }
        return inventory;
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)) {
            throw new BusinessException(401, "Authentication is required");
        }
        return authenticatedUser.id();
    }

    private void throwNotFound() {
        throw new BusinessException(404, "Inventory item not found");
    }

    private InventoryResponse toResponse(Inventory inventory) {
        return new InventoryResponse(
                inventory.getId(),
                inventory.getFoodName(),
                inventory.getQuantity(),
                inventory.getUnit(),
                inventory.getAddedDate(),
                inventory.getReminderDays(),
                inventory.getCreateTime(),
                inventory.getUpdateTime());
    }
}
