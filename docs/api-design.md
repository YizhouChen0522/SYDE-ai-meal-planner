# API Design

All endpoints return a consistent `Result<T>` response:

```json
{
  "code": 200,
  "data": {},
  "message": "success"
}
```

For errors, use the same shape:

```json
{
  "code": 400,
  "data": null,
  "message": "Validation failed"
}
```

## General Rules

- Base path: `/api`.
- Authentication uses JWT.
- Protected endpoints require `Authorization: Bearer <token>`.
- The backend derives `user_id` from JWT.
- The frontend must not send `userId` for user-owned operations.
- Passwords are stored with BCrypt.
- Numeric quantities are rounded to 2 decimals.
- Inventory and shopping list matching use normalized name plus normalized exact unit.
- No unit conversion is supported in the first MVP.

## Authentication

### POST /api/auth/register

Creates a new user.

Authorization: public.

Request:

```json
{
  "username": "Demo User",
  "email": "student@example.com",
  "password": "password123"
}
```

Response:

```json
{
  "code": 200,
  "data": {
    "id": 1,
    "username": "Demo User",
    "email": "student@example.com"
  },
  "message": "success"
}
```

Validation:

- `username` is required, max 100 characters.
- `email` is required, valid email format, unique, max 255 characters.
- `password` is required, recommended minimum 6 characters for MVP.

Errors:

- `400`: invalid request fields.
- `409`: email already registered.

### POST /api/auth/login

Authenticates a user and returns a JWT.

Authorization: public.

Request:

```json
{
  "email": "student@example.com",
  "password": "password123"
}
```

Response:

```json
{
  "code": 200,
  "data": {
    "token": "jwt-token",
    "user": {
      "id": 1,
      "username": "Demo User",
      "email": "student@example.com"
    }
  },
  "message": "success"
}
```

Validation:

- `email` is required.
- `password` is required.

Errors:

- `400`: missing fields.
- `401`: invalid email/password.
- `403`: user disabled.

### GET /api/auth/me

Returns the authenticated user.

Authorization: required.

Response:

```json
{
  "code": 200,
  "data": {
    "id": 1,
    "username": "Demo User",
    "email": "student@example.com"
  },
  "message": "success"
}
```

Errors:

- `401`: missing, invalid, or expired token.

## Profile

### GET /api/profile

Returns the authenticated user's profile. If no row exists yet, the backend may create or return default values.

Authorization: required.

Response:

```json
{
  "code": 200,
  "data": {
    "likedFoods": [],
    "dislikedFoods": [],
    "allergies": [],
    "dietaryRestrictions": [],
    "flavorPreferences": [],
    "cookingSkill": "",
    "servingSize": 2,
    "maxPrepTime": 30,
    "budget": "",
    "equipment": []
  },
  "message": "success"
}
```

### PUT /api/profile

Updates the authenticated user's profile.

Authorization: required.

Request:

```json
{
  "likedFoods": ["Chicken", "Tofu"],
  "dislikedFoods": ["Mushrooms"],
  "allergies": ["Peanuts"],
  "dietaryRestrictions": ["Vegetarian"],
  "flavorPreferences": ["Spicy", "Savory"],
  "cookingSkill": "Beginner",
  "servingSize": 2,
  "maxPrepTime": 30,
  "budget": "Medium",
  "equipment": ["Stovetop", "Oven"]
}
```

Response: same structure as `GET /api/profile`.

Validation:

- Array fields must be arrays of strings.
- `servingSize` must be between 1 and 12.
- `maxPrepTime` must be between 10 and 180.
- `cookingSkill` should be blank, `Beginner`, `Intermediate`, or `Advanced`.
- `budget` should be blank, `Low`, `Medium`, or `Flexible`.

Errors:

- `400`: invalid profile fields.
- `401`: unauthenticated.

## Inventory

Inventory item response:

```json
{
  "id": 10,
  "name": "Chicken breast",
  "quantity": 0.8,
  "unit": "kg",
  "addedDate": "2026-06-10"
}
```

### GET /api/inventory

Lists the authenticated user's inventory.

Authorization: required.

Response:

```json
{
  "code": 200,
  "data": [
    {
      "id": 10,
      "name": "Chicken breast",
      "quantity": 0.8,
      "unit": "kg",
      "addedDate": "2026-06-10"
    }
  ],
  "message": "success"
}
```

### POST /api/inventory

Adds an item or merges it into an existing item with the same normalized name and normalized exact unit.

Authorization: required.

Request:

```json
{
  "name": "Chicken breast",
  "quantity": 0.5,
  "unit": "kg",
  "addedDate": "2026-07-16"
}
```

Response:

```json
{
  "code": 200,
  "data": {
    "id": 10,
    "name": "Chicken breast",
    "quantity": 1.3,
    "unit": "kg",
    "addedDate": "2026-06-10"
  },
  "message": "success"
}
```

Validation:

- `name` is required.
- `quantity` must be greater than 0.
- `unit` is required.
- `addedDate` is optional; backend defaults to current date.

Errors:

- `400`: invalid item fields.
- `401`: unauthenticated.

### PATCH /api/inventory/{id}/consume

Marks a percentage of an item as consumed. If `percent` is 100, the item is deleted.

Authorization: required.

Request:

```json
{
  "percent": 25
}
```

Response:

```json
{
  "code": 200,
  "data": {
    "id": 10,
    "name": "Chicken breast",
    "quantity": 0.6,
    "unit": "kg",
    "addedDate": "2026-06-10"
  },
  "message": "success"
}
```

Validation:

- `percent` must be greater than 0 and less than or equal to 100.

Errors:

- `400`: invalid percent.
- `401`: unauthenticated.
- `404`: item not found for current user.

### DELETE /api/inventory/{id}

Deletes an inventory item owned by the authenticated user.

Authorization: required.

Response:

```json
{
  "code": 200,
  "data": true,
  "message": "success"
}
```

Errors:

- `401`: unauthenticated.
- `404`: item not found for current user.

## Meal Plans

Recipe object:

```json
{
  "id": "recipe-1",
  "title": "Lemon Chickpea Power Bowl",
  "ingredients": [
    { "name": "Chickpeas", "quantity": 0.3, "unit": "kg" }
  ],
  "steps": ["Warm chickpeas."],
  "nutrition": {
    "calories": { "amount": 520, "unit": "kcal", "dailyValuePercent": 26 }
  }
}
```

### POST /api/meal-plans/generate

Generates recipes. For the first backend MVP, this endpoint may return backend mock recipes. Real LLM integration comes last.

Authorization: required.

Request:

```json
{
  "desiredFoodInput": "I want spicy Chinese food, chicken, noodles, or a quick dinner."
}
```

Response:

```json
{
  "code": 200,
  "data": {
    "desiredFoodInput": "I want spicy Chinese food, chicken, noodles, or a quick dinner.",
    "recipes": []
  },
  "message": "success"
}
```

Validation:

- `desiredFoodInput` is optional for MVP but should be max 1000 characters.

Errors:

- `400`: input too long.
- `401`: unauthenticated.

### POST /api/meal-plans/confirm

Confirms the current frontend recipe selection, stores meal plan history, and generates or merges shopping list items. Recipe editing before this request remains frontend-only state.

Authorization: required.

Request:

```json
{
  "desiredFoodInput": "Quick dinner with chicken",
  "recipes": [
    {
      "id": "recipe-1",
      "title": "Chicken Rice Bowl",
      "ingredients": [
        { "name": "Chicken breast", "quantity": 0.3, "unit": "kg" }
      ],
      "steps": ["Cook chicken."],
      "nutrition": {
        "calories": { "amount": 520, "unit": "kcal", "dailyValuePercent": 26 }
      }
    }
  ]
}
```

Response:

```json
{
  "code": 200,
  "data": {
    "mealPlanId": 20,
    "confirmedTime": "2026-07-16T12:00:00",
    "shoppingList": []
  },
  "message": "success"
}
```

Validation:

- `recipes` is required and must contain at least 1 recipe.
- Each recipe must have `title`.
- Each ingredient must have `name`, `quantity > 0`, and `unit`.

Errors:

- `400`: invalid recipe payload.
- `401`: unauthenticated.

### GET /api/meal-plans/history

Lists confirmed meal plans for the authenticated user.

Authorization: required.

Response:

```json
{
  "code": 200,
  "data": [
    {
      "id": 20,
      "desiredFoodInput": "Quick dinner with chicken",
      "recipes": [],
      "confirmedTime": "2026-07-16T12:00:00"
    }
  ],
  "message": "success"
}
```

Errors:

- `401`: unauthenticated.

### GET /api/meal-plans/{id}

Returns one confirmed meal plan owned by the authenticated user.

Authorization: required.

Response:

```json
{
  "code": 200,
  "data": {
    "id": 20,
    "desiredFoodInput": "Quick dinner with chicken",
    "recipes": [],
    "confirmedTime": "2026-07-16T12:00:00"
  },
  "message": "success"
}
```

Errors:

- `401`: unauthenticated.
- `404`: meal plan not found for current user.

## Shopping List

Shopping list item response:

```json
{
  "id": 30,
  "name": "Chicken breast",
  "requiredQuantity": 0.3,
  "availableQuantity": 0.1,
  "quantityToBuy": 0.2,
  "unit": "kg"
}
```

### GET /api/shopping-list

Lists the active shopping list for the authenticated user.

Authorization: required.

Response:

```json
{
  "code": 200,
  "data": [
    {
      "id": 30,
      "name": "Chicken breast",
      "requiredQuantity": 0.3,
      "availableQuantity": 0.1,
      "quantityToBuy": 0.2,
      "unit": "kg"
    }
  ],
  "message": "success"
}
```

Errors:

- `401`: unauthenticated.

### DELETE /api/shopping-list/{id}

Deletes one shopping list item owned by the authenticated user.

Authorization: required.

Response:

```json
{
  "code": 200,
  "data": true,
  "message": "success"
}
```

Errors:

- `401`: unauthenticated.
- `404`: shopping list item not found for current user.

### POST /api/shopping-list/place-order

Places the MVP mock order. This must be transactional: add or merge purchased items into inventory, then clear the user's shopping list.

Authorization: required.

Request:

```json
{}
```

Response:

```json
{
  "code": 200,
  "data": {
    "addedInventoryItems": [
      {
        "id": 10,
        "name": "Chicken breast",
        "quantity": 1.3,
        "unit": "kg",
        "addedDate": "2026-07-16"
      }
    ],
    "shoppingListCleared": true
  },
  "message": "success"
}
```

Validation:

- Shopping list must contain at least one item with `quantityToBuy > 0`.

Errors:

- `400`: shopping list is empty.
- `401`: unauthenticated.
- `500`: transaction failed; inventory and shopping list should remain unchanged.
