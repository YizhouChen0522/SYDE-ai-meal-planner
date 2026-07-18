package com.syde.mealplanner.controller;

import com.syde.mealplanner.common.Result;
import com.syde.mealplanner.dto.ShoppingListResponse;
import com.syde.mealplanner.service.ShoppingListService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/shopping-list")
public class ShoppingListController {

    private final ShoppingListService shoppingListService;

    public ShoppingListController(ShoppingListService shoppingListService) {
        this.shoppingListService = shoppingListService;
    }

    @GetMapping
    public Result<List<ShoppingListResponse>> getAll() {
        return Result.success(shoppingListService.getAll());
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        shoppingListService.delete(id);
        return Result.success();
    }

    @PostMapping("/place-order")
    public Result<Void> placeOrder() {
        shoppingListService.placeOrder();
        return Result.success();
    }
}
