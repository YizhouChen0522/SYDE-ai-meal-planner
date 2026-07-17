package com.syde.mealplanner.controller;

import com.syde.mealplanner.common.Result;
import com.syde.mealplanner.dto.LoginRequest;
import com.syde.mealplanner.dto.LoginResponse;
import com.syde.mealplanner.dto.RegisterRequest;
import com.syde.mealplanner.dto.UserSummaryResponse;
import com.syde.mealplanner.security.AuthenticatedUser;
import com.syde.mealplanner.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Result<UserSummaryResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @GetMapping("/me")
    public Result<UserSummaryResponse> me(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return Result.success(authService.getCurrentUser(authenticatedUser));
    }
}
