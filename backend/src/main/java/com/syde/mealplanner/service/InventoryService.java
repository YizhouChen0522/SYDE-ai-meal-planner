package com.syde.mealplanner.service;

import com.syde.mealplanner.dto.InventoryCreateRequest;
import com.syde.mealplanner.dto.InventoryResponse;
import com.syde.mealplanner.dto.InventoryUpdateRequest;

import java.util.List;

public interface InventoryService {

    List<InventoryResponse> getAll();

    InventoryResponse getById(Long id);

    InventoryResponse create(InventoryCreateRequest request);

    InventoryResponse update(Long id, InventoryUpdateRequest request);

    void delete(Long id);
}
