package com.syde.mealplanner.service;

import com.syde.mealplanner.dto.UserProfileResponse;
import com.syde.mealplanner.dto.UserProfileUpdateRequest;

public interface UserProfileService {

    UserProfileResponse getCurrentUserProfile();

    UserProfileResponse updateCurrentUserProfile(UserProfileUpdateRequest request);
}
