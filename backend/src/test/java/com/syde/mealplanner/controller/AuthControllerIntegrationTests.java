package com.syde.mealplanner.controller;

import com.jayway.jsonpath.JsonPath;
import com.syde.mealplanner.config.JwtProperties;
import com.syde.mealplanner.entity.User;
import com.syde.mealplanner.entity.UserProfile;
import com.syde.mealplanner.mapper.UserMapper;
import com.syde.mealplanner.mapper.UserProfileMapper;
import com.syde.mealplanner.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTests {

    private static final String RAW_PASSWORD = "password123";
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserProfileMapper userProfileMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtProperties jwtProperties;

    @Test
    void registerCreatesUserWithBcryptHashAndEmptyProfile() throws Exception {
        String email = uniqueEmail().toUpperCase();
        String normalizedEmail = email.toLowerCase();

        String responseBody = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "  Johnny  ",
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, RAW_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.username").value("Johnny"))
                .andExpect(jsonPath("$.data.email").value(normalizedEmail))
                .andExpect(jsonPath("$.data.status").value(1))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number userId = JsonPath.read(responseBody, "$.data.id");
        User savedUser = userMapper.selectById(userId.longValue());
        assertNotNull(savedUser);
        assertEquals("Johnny", savedUser.getUsername());
        assertEquals(normalizedEmail, savedUser.getEmail());
        assertNotEquals(RAW_PASSWORD, savedUser.getPassword());
        assertTrue(passwordEncoder.matches(RAW_PASSWORD, savedUser.getPassword()));

        UserProfile profile = userProfileMapper.selectByUserId(savedUser.getId());
        assertNotNull(profile);
        assertEquals(0, profile.getLikedFoods().size());
        assertEquals(0, profile.getDislikedFoods().size());
        assertEquals(0, profile.getAllergies().size());
    }

    @Test
    void registerRejectsDuplicateNormalizedEmail() throws Exception {
        String email = uniqueEmail();
        registerUser("Duplicate User", email, RAW_PASSWORD);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "Duplicate User",
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email.toUpperCase(), RAW_PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void loginReturnsJwtAndGenericErrorsForUnknownEmailAndWrongPassword() throws Exception {
        String email = uniqueEmail();
        Long userId = registerUser("Login User", email.toUpperCase(), RAW_PASSWORD);

        String responseBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email.toUpperCase(), RAW_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(86400000))
                .andExpect(jsonPath("$.data.user.id").value(userId))
                .andExpect(jsonPath("$.data.user.email").value(email))
                .andExpect(jsonPath("$.data.user.password").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = JsonPath.read(responseBody, "$.data.token");
        assertFalse(token.isBlank());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "wrong-password"
                                }
                                """.formatted(email)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "wrong-password"
                                }
                                """.formatted(uniqueEmail())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void loginRejectsDisabledUser() throws Exception {
        String email = uniqueEmail();
        Long userId = registerUser("Disabled User", email, RAW_PASSWORD);
        assertEquals(1, userMapper.updateStatusById(userId, 0));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, RAW_PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void meRequiresValidJwtAndDerivesUserFromAuthentication() throws Exception {
        String email = uniqueEmail();
        Long userId = registerUser("Me User", email, RAW_PASSWORD);
        String token = loginAndReadToken(email, RAW_PASSWORD);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(userId))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void meRejectsMalformedExpiredDisabledAndNonexistentUserTokens() throws Exception {
        String email = uniqueEmail();
        Long userId = registerUser("Token User", email, RAW_PASSWORD);
        String validToken = loginAndReadToken(email, RAW_PASSWORD);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer not-a-valid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        JwtService expiredJwtService = new JwtService(new JwtProperties(jwtProperties.secret(), -1000));
        User user = userMapper.selectById(userId);
        String expiredToken = expiredJwtService.generateToken(user);
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        assertEquals(1, userMapper.updateStatusById(userId, 0));
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        User nonexistentUser = new User();
        nonexistentUser.setId(Long.MAX_VALUE);
        nonexistentUser.setEmail(uniqueEmail());
        String nonexistentUserToken = jwtService.generateToken(nonexistentUser);
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + nonexistentUserToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void publicAuthAndHealthEndpointsRemainAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "Public User",
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(uniqueEmail(), RAW_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        String email = uniqueEmail();
        registerUser("Public Login User", email, RAW_PASSWORD);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, RAW_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    private Long registerUser(String username, String email, String password) throws Exception {
        String responseBody = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, email, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number userId = JsonPath.read(responseBody, "$.data.id");
        return userId.longValue();
    }

    private String loginAndReadToken(String email, String password) throws Exception {
        String responseBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return JsonPath.read(responseBody, "$.data.token");
    }

    private String uniqueEmail() {
        return "auth-test-" + UUID.randomUUID() + "@example.com";
    }
}
