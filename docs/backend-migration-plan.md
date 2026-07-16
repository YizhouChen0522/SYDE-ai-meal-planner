# Backend Migration Plan

This plan maps the current Vue/localStorage prototype to a Spring Boot backend. Frontend mocks should remain in place until the corresponding backend module is implemented, tested, and connected from the frontend.

## Current Frontend Storage Mapping

| Frontend localStorage key | Current purpose | Backend replacement |
|---|---|---|
| `syde-auth-token` | Mock token string | JWT returned by `POST /api/auth/login` |
| `syde-current-user` | Mock current user object | `GET /api/auth/me` |
| `syde-user-profile` | Food preference profile | `GET /api/profile`, `PUT /api/profile` |
| `syde-inventory-items` | Inventory item array | `GET /api/inventory`, `POST /api/inventory`, `PATCH /api/inventory/{id}/consume`, `DELETE /api/inventory/{id}` |
| `currentMealPlanDraft` | Frontend draft meal plan | Keep frontend-only; no MVP draft API |
| `syde-meal-plan-history` | Confirmed meal plan history | `POST /api/meal-plans/confirm`, `GET /api/meal-plans/history`, `GET /api/meal-plans/{id}` |
| `syde-shopping-list` | Active shopping list | `GET /api/shopping-list`, `DELETE /api/shopping-list/{id}`, `POST /api/shopping-list/place-order` |

## Frontend Feature Mapping

| Current frontend feature | Current location | Future endpoint |
|---|---|---|
| Register | `authStore.register` | `POST /api/auth/register` |
| Login | `authStore.login` | `POST /api/auth/login` |
| Load current user | `authStore.loadAuthFromStorage` | `GET /api/auth/me` |
| Save profile | `authStore.saveProfile` | `PUT /api/profile` |
| Load profile | `authStore.loadAuthFromStorage` | `GET /api/profile` |
| List inventory | `inventoryStore.loadInventoryFromStorage` | `GET /api/inventory` |
| Add inventory item | `inventoryStore.addItem` | `POST /api/inventory` |
| Merge ordered inventory item | `inventoryStore.addOrMergeItem` | `POST /api/inventory` |
| Consume inventory item | `inventoryStore.applyConsumption` | `PATCH /api/inventory/{id}/consume` |
| Delete inventory item | `inventoryStore.removeItem` | `DELETE /api/inventory/{id}` |
| Generate recipes | `MealPlannerView.generateRecipes` | `POST /api/meal-plans/generate` |
| Edit generated recipes | `MealPlannerView` local state | Keep frontend state for MVP |
| Confirm meal plan | `MealPlannerView.confirmMealPlan` | `POST /api/meal-plans/confirm` |
| Load history | `mealPlanStore.loadHistoryFromStorage` | `GET /api/meal-plans/history` |
| View history detail | `HistoryView` stored record | `GET /api/meal-plans/{id}` |
| Generate shopping list | `shoppingListStore.generateShoppingList` | Backend side effect of `POST /api/meal-plans/confirm` |
| Load shopping list | `shoppingListStore.loadShoppingListFromStorage` | `GET /api/shopping-list` |
| Delete shopping item | `shoppingListStore.removeShoppingListItem` | `DELETE /api/shopping-list/{id}` |
| Place mock order | `ShoppingListView.placeMockOrder` | `POST /api/shopping-list/place-order` |

## Migration Order

### 1. Backend Skeleton

Create the Spring Boot project structure, MySQL configuration, common `Result<T>` response wrapper, exception handling, and basic health check.

Frontend mock status:

- All frontend mocks remain.
- No frontend API calls are required yet.

### 2. Auth

Implement:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`
- JWT issuing and validation.
- BCrypt password hashing.
- `sys_user` table.

Frontend migration:

- Replace mock register/login with real API calls.
- Continue storing the returned JWT client-side.
- Replace mock current user loading with `/api/auth/me`.

Frontend mock status:

- Profile, inventory, meal plan, shopping list, and history mocks remain.

### 3. Profile

Implement:

- `GET /api/profile`
- `PUT /api/profile`
- `user_profile` table.
- JSON storage for profile arrays.

Frontend migration:

- Replace `syde-user-profile` reads/writes with profile API calls.
- Keep the same frontend field names where possible: `likedFoods`, `dietaryRestrictions`, `maxPrepTime`, etc.

Frontend mock status:

- Inventory, meal generation, shopping list, and history mocks remain.

### 4. Inventory

Implement:

- `GET /api/inventory`
- `POST /api/inventory`
- `PATCH /api/inventory/{id}/consume`
- `DELETE /api/inventory/{id}`
- `inventory_item` table.
- Merge by authenticated user, normalized name, and normalized exact unit.

Frontend migration:

- Replace `syde-inventory-items`.
- Keep storage reminder calculation in frontend initially, or return `addedDate` and let the existing UI calculate it.

Frontend mock status:

- Meal generation, shopping list, place order, and history mocks remain.

### 5. Backend Mock Meal Generation

Implement:

- `POST /api/meal-plans/generate`
- Backend mock recipe list equivalent to the current frontend mock recipes.

Frontend migration:

- Replace inline mock recipe generation with the backend response.
- Keep recipe editing before confirmation in frontend local state.

Frontend mock status:

- Replace/add/delete recipe behavior can remain frontend-only.
- Shopping list and history localStorage can remain until confirm/history is ready.

### 6. Meal Plan Confirm And History

Implement:

- `POST /api/meal-plans/confirm`
- `GET /api/meal-plans/history`
- `GET /api/meal-plans/{id}`
- `meal_plan` table.
- Store confirmed `desired_food_input` and `recipes_json`.

Important MVP decision:

- Do not store the entire global shopping cart as a meal-plan history snapshot.
- History stores confirmed recipes and desired food input.
- Shopping list items are managed separately as active shopping list state.

Frontend migration:

- Replace `syde-meal-plan-history`.
- Remove dependency on saved `shoppingListSnapshot` for new backend-backed history records.
- History view should display recipes from `recipes_json`.

Frontend mock status:

- Shopping list and place order mocks can remain until the shopping list module is connected.

### 7. Shopping List And Place Order

Implement:

- Active shopping list generation as part of `POST /api/meal-plans/confirm`.
- `GET /api/shopping-list`
- `DELETE /api/shopping-list/{id}`
- `POST /api/shopping-list/place-order`
- `shopping_list_item` table.

Confirm behavior:

- Aggregate confirmed recipe ingredients by normalized name and normalized exact unit.
- Compare against current inventory.
- Calculate `quantity_to_buy = max(0, required_quantity - available_quantity)`.
- Merge repeated confirmations into active shopping list by user, normalized name, and unit.

Place order behavior:

- Run in one transaction.
- Add or merge `quantity_to_buy` into inventory.
- Clear the user's shopping list.
- Roll back all changes if any step fails.

Frontend migration:

- Replace `syde-shopping-list`.
- Replace frontend `placeMockOrder` inventory merge logic with the backend endpoint.

Frontend mock status:

- LLM generation remains mocked until the final integration step.

### 8. Real LLM Integration

Implement:

- Real recipe generation behind `POST /api/meal-plans/generate`.
- Prompt construction using desired food input, profile, and inventory.
- JSON schema validation for generated recipes.
- Fallback to backend mock recipes if the LLM request fails.

Frontend migration:

- No endpoint change should be needed if the mock backend response already matches the final recipe shape.

Frontend mock status:

- Backend mock recipes can remain as fallback only.

### 9. Remove Remaining Frontend Mocks

After each backend module is tested and connected, remove the corresponding localStorage dependency and mock behavior.

Final cleanup targets:

- Mock token `mock-jwt-token`.
- Mock current user object.
- Frontend mock recipe array, except maybe test fixtures.
- localStorage persistence for profile, inventory, shopping list, and history.
- Frontend shopping list aggregation logic.
- Frontend place order merge logic.

## Data Consistency Rules To Preserve

- User-owned operations must always use the JWT user id.
- Inventory and shopping list matching must use normalized name plus normalized exact unit.
- No `g`/`kg` conversion in MVP.
- Confirmed meal plans are immutable history records for MVP.
- Recipe editing before confirmation remains frontend state.
- Place order must be transactional.
- Repeated meal confirmations merge into the active shopping list, not into meal plan history.

## Unresolved Or Later Decisions

- Whether JWT should be stored in localStorage or a secure cookie.
- Whether storage freshness reminders should stay frontend-only or become backend derived fields.
- Whether profile free-text fields should remain arrays long term.
- Whether future versions should normalize units and ingredient synonyms.
- Whether later versions need separate recipe, nutrition, order, and order-item tables.
