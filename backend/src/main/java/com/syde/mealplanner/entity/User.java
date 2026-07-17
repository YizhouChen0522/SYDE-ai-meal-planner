package com.syde.mealplanner.entity;

import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
public class User {

    private Long id;
    private String username;
    private String email;
    @ToString.Exclude
    private String password;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
