package com.syde.mealplanner.service.impl;

import com.syde.mealplanner.dto.UserProfileResponse;
import com.syde.mealplanner.dto.UserProfileUpdateRequest;
import com.syde.mealplanner.entity.UserProfile;
import com.syde.mealplanner.exception.BusinessException;
import com.syde.mealplanner.mapper.UserProfileMapper;
import com.syde.mealplanner.security.AuthenticatedUser;
import com.syde.mealplanner.service.UserProfileService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class UserProfileServiceImpl implements UserProfileService {

    private static final int MAX_ITEM_LENGTH = 100;

    private final UserProfileMapper userProfileMapper;

    public UserProfileServiceImpl(UserProfileMapper userProfileMapper) {
        this.userProfileMapper = userProfileMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile() {
        return toResponse(findCurrentUserProfileOrThrow());
    }

    @Override
    @Transactional
    public UserProfileResponse updateCurrentUserProfile(UserProfileUpdateRequest request) {
        Long userId = getCurrentUserId();
        UserProfile existingProfile = userProfileMapper.selectByUserId(userId);
        if (existingProfile == null) {
            throwNotFound();
        }

        existingProfile.setLikedFoods(normalizeItems(request == null ? null : request.likedFoods()));
        existingProfile.setDislikedFoods(normalizeItems(request == null ? null : request.dislikedFoods()));
        existingProfile.setAllergies(normalizeItems(request == null ? null : request.allergies()));

        if (userProfileMapper.updateByUserId(existingProfile) != 1) {
            throwNotFound();
        }

        return toResponse(userProfileMapper.selectByUserId(userId));
    }

    private UserProfile findCurrentUserProfileOrThrow() {
        UserProfile userProfile = userProfileMapper.selectByUserId(getCurrentUserId());
        if (userProfile == null) {
            throwNotFound();
        }
        return userProfile;
    }

    private List<String> normalizeItems(List<String> items) {
        if (items == null) {
            return List.of();
        }

        List<String> normalizedItems = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String item : items) {
            if (item == null || item.trim().isEmpty()) {
                throw new BusinessException(400, "Profile item must not be blank");
            }

            String trimmedItem = item.trim();
            if (trimmedItem.length() > MAX_ITEM_LENGTH) {
                throw new BusinessException(400, "Profile item must be at most 100 characters");
            }

            String normalizedKey = trimmedItem.toLowerCase(Locale.ROOT);
            if (seen.add(normalizedKey)) {
                normalizedItems.add(trimmedItem);
            }
        }

        return List.copyOf(normalizedItems);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)) {
            throw new BusinessException(401, "Authentication is required");
        }
        return authenticatedUser.id();
    }

    private UserProfileResponse toResponse(UserProfile userProfile) {
        return new UserProfileResponse(
                nullToEmpty(userProfile.getLikedFoods()),
                nullToEmpty(userProfile.getDislikedFoods()),
                nullToEmpty(userProfile.getAllergies()));
    }

    private List<String> nullToEmpty(List<String> items) {
        return items == null ? List.of() : items;
    }

    private void throwNotFound() {
        throw new BusinessException(404, "User profile not found");
    }
}
