package com.syde.mealplanner.service.impl;

import com.syde.mealplanner.dto.LoginRequest;
import com.syde.mealplanner.dto.LoginResponse;
import com.syde.mealplanner.dto.RegisterRequest;
import com.syde.mealplanner.dto.UserSummaryResponse;
import com.syde.mealplanner.entity.User;
import com.syde.mealplanner.entity.UserProfile;
import com.syde.mealplanner.exception.BusinessException;
import com.syde.mealplanner.mapper.UserMapper;
import com.syde.mealplanner.mapper.UserProfileMapper;
import com.syde.mealplanner.security.AuthenticatedUser;
import com.syde.mealplanner.security.JwtService;
import com.syde.mealplanner.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class AuthServiceImpl implements AuthService {

    private static final int ACTIVE_STATUS = 1;
    private static final int DISABLED_STATUS = 0;
    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password";

    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(
            UserMapper userMapper,
            UserProfileMapper userProfileMapper,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userMapper = userMapper;
        this.userProfileMapper = userProfileMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public UserSummaryResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        String email = normalizeEmail(request.getEmail());

        if (userMapper.selectByEmail(email) != null) {
            throw new BusinessException(409, "Email is already registered");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(ACTIVE_STATUS);

        if (userMapper.insert(user) != 1 || user.getId() == null) {
            throw new BusinessException(500, "Could not create user");
        }

        UserProfile userProfile = new UserProfile();
        userProfile.setUserId(user.getId());
        userProfile.setLikedFoods(List.of());
        userProfile.setDislikedFoods(List.of());
        userProfile.setAllergies(List.of());

        if (userProfileMapper.insert(userProfile) != 1) {
            throw new BusinessException(500, "Could not create user profile");
        }

        return toUserSummary(userMapper.selectById(user.getId()));
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());
        User user = userMapper.selectByEmail(email);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, INVALID_CREDENTIALS_MESSAGE);
        }

        if (Integer.valueOf(DISABLED_STATUS).equals(user.getStatus())) {
            throw new BusinessException(403, "Account is disabled");
        }

        String token = jwtService.generateToken(user);
        return new LoginResponse(token, "Bearer", jwtService.getExpirationMs(), toUserSummary(user));
    }

    @Override
    public UserSummaryResponse getCurrentUser(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.id() == null) {
            throw new BusinessException(401, "Authentication is required");
        }

        User user = userMapper.selectById(authenticatedUser.id());
        if (user == null) {
            throw new BusinessException(401, "Authentication is required");
        }

        if (Integer.valueOf(DISABLED_STATUS).equals(user.getStatus())) {
            throw new BusinessException(403, "Account is disabled");
        }

        return toUserSummary(user);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private UserSummaryResponse toUserSummary(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getStatus(),
                user.getCreateTime());
    }
}
