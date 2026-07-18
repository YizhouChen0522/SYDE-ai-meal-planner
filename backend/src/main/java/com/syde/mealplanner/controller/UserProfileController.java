package com.syde.mealplanner.controller;

import com.syde.mealplanner.common.Result;
import com.syde.mealplanner.dto.UserProfileResponse;
import com.syde.mealplanner.dto.UserProfileUpdateRequest;
import com.syde.mealplanner.service.UserProfileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public Result<UserProfileResponse> getCurrentProfile() {
        return Result.success(userProfileService.getCurrentUserProfile());
    }

    @PutMapping
    public Result<UserProfileResponse> updateCurrentProfile(@RequestBody(required = false) UserProfileUpdateRequest request) {
        return Result.success(userProfileService.updateCurrentUserProfile(request));
    }
}
