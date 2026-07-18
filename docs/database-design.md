# Database Design

This document defines the approved MySQL database design for the Spring Boot course MVP. The schema keeps the first backend version small: user-owned data is scoped by `user_id`, recipes are stored as JSON, and order/recipe detail tables are deferred.

## Design Principles

- Use JWT authentication in the backend.
- Derive the current user id from the JWT for all user-owned operations.
- Never accept arbitrary `userId` from the frontend for profile, inventory, meal plan, or shopping list operations.
- Use BCrypt for password storage.
- Use MySQL JSON columns for profile arrays and confirmed recipes.
- Do not create draft, recipe, nutrition, order, or order-item tables for the first MVP.
- Maintain one inventory per user.
- Match inventory by normalized ingredient name plus normalized exact unit.
- Do not perform unit conversion such as `g` to `kg` in the first MVP.

## Ownership Relationships

| Table | Owner |
|---|---|
| `user_profile` | `user_profile.user_id -> sys_user.id` |
| `inventory_item` | `inventory_item.user_id -> sys_user.id` |
| `meal_plan` | `meal_plan.user_id -> sys_user.id` |
| `shopping_list_item` | `shopping_list_item.user_id -> sys_user.id` |

Each user can have one profile, many inventory items, many confirmed meal plans, and many active shopping list items.

## Table: sys_user

Stores registered users and authentication information.

| Field | Type | Notes |
|---|---|---|
| `id` | BIGINT | Primary key |
| `username` | VARCHAR | Display name |
| `email` | VARCHAR | Login identifier; unique |
| `password` | VARCHAR | BCrypt password hash |
| `status` | TINYINT | `1` active, `0` disabled |
| `create_time` | DATETIME | Created timestamp |
| `update_time` | DATETIME | Updated timestamp |

```sql
CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(100) NOT NULL,
  email VARCHAR(255) NOT NULL,
  password VARCHAR(255) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_sys_user_email (email),
  KEY idx_sys_user_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## Table: user_profile

Stores food preferences and meal planning constraints. JSON columns are used for arrays so the MVP can preserve the frontend shape without extra lookup tables.

| Field | Type | Notes |
|---|---|---|
| `id` | BIGINT | Primary key |
| `user_id` | BIGINT | Owner |
| `liked_foods` | JSON | Array of strings |
| `disliked_foods` | JSON | Array of strings |
| `allergies` | JSON | Array of strings |
| `dietary_restrictions` | JSON | Array of strings |
| `flavor_preferences` | JSON | Array of strings |
| `cooking_skill` | VARCHAR | `Beginner`, `Intermediate`, `Advanced`, or blank |
| `serving_size` | INT | Number of servings |
| `max_prep_time` | INT | Minutes |
| `budget` | VARCHAR | `Low`, `Medium`, `Flexible`, or blank |
| `equipment` | JSON | Array of strings |
| `create_time` | DATETIME | Created timestamp |
| `update_time` | DATETIME | Updated timestamp |

```sql
CREATE TABLE user_profile (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  liked_foods JSON NULL,
  disliked_foods JSON NULL,
  allergies JSON NULL,
  dietary_restrictions JSON NULL,
  flavor_preferences JSON NULL,
  cooking_skill VARCHAR(50) NULL,
  serving_size INT NOT NULL DEFAULT 2,
  max_prep_time INT NOT NULL DEFAULT 30,
  budget VARCHAR(50) NULL,
  equipment JSON NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_profile_user_id (user_id),
  CONSTRAINT fk_user_profile_user
    FOREIGN KEY (user_id) REFERENCES sys_user (id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## Table: inventory_item

Stores the user's virtual fridge and pantry items.

| Field | Type | Notes |
|---|---|---|
| `id` | BIGINT | Primary key |
| `user_id` | BIGINT | Owner |
| `name` | VARCHAR | Display name |
| `normalized_name` | VARCHAR | Trimmed lowercase name for matching |
| `quantity` | DECIMAL | Current amount |
| `unit` | VARCHAR | Normalized exact unit, such as `kg`, `g`, `piece` |
| `added_date` | DATE | Date the item entered inventory |
| `create_time` | DATETIME | Created timestamp |
| `update_time` | DATETIME | Updated timestamp |

The unique key prevents duplicate inventory rows for the same normalized item and unit.

```sql
CREATE TABLE inventory_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  name VARCHAR(150) NOT NULL,
  normalized_name VARCHAR(150) NOT NULL,
  quantity DECIMAL(10,2) NOT NULL,
  unit VARCHAR(50) NOT NULL,
  added_date DATE NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_inventory_user_name_unit (user_id, normalized_name, unit),
  KEY idx_inventory_user (user_id),
  CONSTRAINT fk_inventory_user
    FOREIGN KEY (user_id) REFERENCES sys_user (id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## Table: meal_plan

Stores confirmed meal plans. Recipe editing before confirmation remains frontend state, so only confirmed plans are persisted.

| Field | Type | Notes |
|---|---|---|
| `id` | BIGINT | Primary key |
| `user_id` | BIGINT | Owner |
| `desired_food_input` | TEXT | User prompt/request |
| `recipes_json` | JSON | Confirmed recipe array |
| `confirmed_time` | DATETIME | Time the plan was confirmed |
| `create_time` | DATETIME | Created timestamp |
| `update_time` | DATETIME | Updated timestamp |

`recipes_json` should preserve the frontend recipe structure:

```json
[
  {
    "id": "backend-or-mock-id",
    "title": "Recipe name",
    "ingredients": [{ "name": "Rice", "quantity": 0.2, "unit": "kg" }],
    "steps": ["Cook rice."],
    "nutrition": {
      "calories": { "amount": 520, "unit": "kcal", "dailyValuePercent": 26 }
    }
  }
]
```

```sql
CREATE TABLE meal_plan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  desired_food_input TEXT NULL,
  recipes_json JSON NOT NULL,
  confirmed_time DATETIME NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_meal_plan_user_confirmed_time (user_id, confirmed_time),
  CONSTRAINT fk_meal_plan_user
    FOREIGN KEY (user_id) REFERENCES sys_user (id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## Table: shopping_list_item

Stores the current active shopping list for a user.

| Field | Type | Notes |
|---|---|---|
| `id` | BIGINT | Primary key |
| `user_id` | BIGINT | Owner |
| `name` | VARCHAR | Display ingredient name |
| `normalized_name` | VARCHAR | Trimmed lowercase name |
| `required_quantity` | DECIMAL | Total required by confirmed recipes |
| `available_quantity` | DECIMAL | Quantity available from inventory at generation time |
| `quantity_to_buy` | DECIMAL | Required minus available, minimum 0 |
| `unit` | VARCHAR | Normalized exact unit |
| `create_time` | DATETIME | Created timestamp |
| `update_time` | DATETIME | Updated timestamp |

The unique key supports merging repeated confirmations into the active shopping list.

```sql
CREATE TABLE shopping_list_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  name VARCHAR(150) NOT NULL,
  normalized_name VARCHAR(150) NOT NULL,
  required_quantity DECIMAL(10,2) NOT NULL,
  available_quantity DECIMAL(10,2) NOT NULL DEFAULT 0,
  quantity_to_buy DECIMAL(10,2) NOT NULL,
  unit VARCHAR(50) NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_shopping_user_name_unit (user_id, normalized_name, unit),
  KEY idx_shopping_user (user_id),
  CONSTRAINT fk_shopping_user
    FOREIGN KEY (user_id) REFERENCES sys_user (id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## Normalization Rules

- `normalized_name = trim(name).toLowerCase()`.
- `unit = trim(unit).toLowerCase()`.
- Matching uses `(user_id, normalized_name, unit)`.
- No plural cleanup, synonym matching, or unit conversion is included in the first MVP.

## Transaction Requirements

`POST /api/shopping-list/place-order` must run in one database transaction:

1. Read current shopping list items for the authenticated user.
2. For each item, add or merge `quantity_to_buy` into `inventory_item`.
3. Clear the user's shopping list.
4. Commit only if all steps succeed.
