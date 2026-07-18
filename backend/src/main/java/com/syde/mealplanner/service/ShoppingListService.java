package com.syde.mealplanner.service;

import com.syde.mealplanner.dto.ShoppingListResponse;

import java.util.List;

public interface ShoppingListService {

    List<ShoppingListResponse> getAll();

    void delete(Long id);

    void placeOrder();
}
