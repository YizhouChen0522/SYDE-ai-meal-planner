package com.syde.mealplanner.controller;

import com.syde.mealplanner.common.Result;
import com.syde.mealplanner.dto.InventoryCreateRequest;
import com.syde.mealplanner.dto.InventoryResponse;
import com.syde.mealplanner.dto.InventoryUpdateRequest;
import com.syde.mealplanner.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public Result<List<InventoryResponse>> getAll() {
        return Result.success(inventoryService.getAll());
    }

    @GetMapping("/{id}")
    public Result<InventoryResponse> getById(@PathVariable Long id) {
        return Result.success(inventoryService.getById(id));
    }

    @PostMapping
    public Result<InventoryResponse> create(@Valid @RequestBody InventoryCreateRequest request) {
        return Result.success(inventoryService.create(request));
    }

    @PutMapping("/{id}")
    public Result<InventoryResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody InventoryUpdateRequest request) {
        return Result.success(inventoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        inventoryService.delete(id);
        return Result.success();
    }
}
