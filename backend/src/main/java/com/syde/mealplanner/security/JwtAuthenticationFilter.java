package com.syde.mealplanner.security;

import com.syde.mealplanner.entity.User;
import com.syde.mealplanner.mapper.UserMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int ACTIVE_STATUS = 1;

    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final SecurityErrorResponseWriter responseWriter;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserMapper userMapper,
            SecurityErrorResponseWriter responseWriter) {
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.responseWriter = responseWriter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            responseWriter.write(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid authentication token");
            return;
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isBlank()) {
            responseWriter.write(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid authentication token");
            return;
        }

        try {
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                AuthenticatedUser tokenUser = jwtService.parseAndValidate(token);
                User user = userMapper.selectById(tokenUser.id());

                if (user == null || !Integer.valueOf(ACTIVE_STATUS).equals(user.getStatus())) {
                    responseWriter.write(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid authentication token");
                    return;
                }

                AuthenticatedUser principal = AuthenticatedUser.from(user);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
            responseWriter.write(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid authentication token");
        }
    }
}
