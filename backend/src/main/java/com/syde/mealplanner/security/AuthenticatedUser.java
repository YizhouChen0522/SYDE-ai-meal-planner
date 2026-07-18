package com.syde.mealplanner.security;

import com.syde.mealplanner.entity.User;

public record AuthenticatedUser(Long id, String email) {

    public static AuthenticatedUser from(User user) {
        return new AuthenticatedUser(user.getId(), user.getEmail());
    }
}
