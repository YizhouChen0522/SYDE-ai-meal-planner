CREATE TABLE `user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'User display name',
  `email` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Login email',
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'BCrypt encrypted password',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT 'Account status: 0-disabled, 1-active',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User account table';

CREATE TABLE `user_profile` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `user_id` bigint(20) NOT NULL COMMENT 'Associated user ID',
  `liked_foods` json DEFAULT NULL COMMENT 'Foods the user likes',
  `disliked_foods` json DEFAULT NULL COMMENT 'Foods the user dislikes',
  `allergies` json DEFAULT NULL COMMENT 'User food allergies',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_profile_user_id` (`user_id`),
  CONSTRAINT `fk_user_profile_user`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='User dietary preference profile';

CREATE TABLE `inventory` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `user_id` bigint(20) NOT NULL COMMENT 'Owner user ID',
  `food_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Food name',
  `quantity` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT 'Current inventory quantity',
  `unit` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Measurement unit',
  `added_date` date NOT NULL COMMENT 'Date the food was added to inventory',
  `reminder_days` int(11) DEFAULT NULL COMMENT 'Recommended storage reminder threshold in days',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_inventory_user_food_unit` (`user_id`,`food_name`,`unit`),
  CONSTRAINT `fk_inventory_user`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='User food inventory';

CREATE TABLE `shopping_list` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `user_id` bigint(20) NOT NULL COMMENT 'Owner user ID',
  `food_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Food or ingredient name',
  `required_quantity` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT 'Total quantity required by confirmed meal plans',
  `available_quantity` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT 'Quantity covered by inventory when the shopping item was calculated',
  `quantity_to_buy` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT 'Remaining quantity that needs to be purchased',
  `unit` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Measurement unit',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shopping_list_user_food_unit` (`user_id`,`food_name`,`unit`),
  CONSTRAINT `fk_shopping_list_user`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='User shopping list';

CREATE TABLE `meal_history` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `user_id` bigint(20) NOT NULL COMMENT 'Owner user ID',
  `user_request` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Original desired food input entered by the user',
  `recipes` json NOT NULL COMMENT 'Confirmed recipe snapshot including titles, ingredients, steps, and nutrition',
  `shopping_list_snapshot` json NOT NULL COMMENT 'Shopping list snapshot generated when the meal plan was confirmed',
  `confirmed_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date and time when the meal plan was confirmed',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Database record creation time',
  PRIMARY KEY (`id`),
  KEY `idx_meal_history_user_confirmed_time` (`user_id`,`confirmed_time`),
  CONSTRAINT `fk_meal_history_user`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Confirmed meal plan history with recipe and shopping list snapshots';

CREATE TABLE `meal_plan_draft` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `user_id` bigint(20) NOT NULL COMMENT 'Owner user ID',
  `user_request` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Original desired food input entered by the user',
  `recipes` json NOT NULL COMMENT 'Current editable meal plan generated by AI including recipes, ingredients, steps, and nutrition',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Database record creation time',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_meal_plan_draft_user` (`user_id`),
  CONSTRAINT `fk_meal_plan_draft_user`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Current editable AI-generated meal plan draft';
