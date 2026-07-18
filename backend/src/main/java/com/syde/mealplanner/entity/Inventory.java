package com.syde.mealplanner.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Inventory {

    private Long id;
    private Long userId;
    private String foodName;
    private BigDecimal quantity;
    private String unit;
    private LocalDate addedDate;
    private Integer reminderDays;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
