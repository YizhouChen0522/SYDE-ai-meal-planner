package com.syde.mealplanner.service;

import com.syde.mealplanner.dto.LoginRequest;
import com.syde.mealplanner.dto.LoginResponse;
import com.syde.mealplanner.dto.RegisterRequest;
import com.syde.mealplanner.dto.UserSummaryResponse;
import com.syde.mealplanner.security.AuthenticatedUser;

public interface AuthService {

    UserSummaryResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    UserSummaryResponse getCurrentUser(AuthenticatedUser authenticatedUser);
}
