
package com.gasagency.controller;

import com.gasagency.dto.request.AuthLoginRequestDTO;
import com.gasagency.dto.response.AuthLoginResponseDTO;
import com.gasagency.dto.request.AuthRegisterRequestDTO;
import com.gasagency.dto.response.UserDTO;
import com.gasagency.service.AuthService;
import com.gasagency.util.ApiResponse;
import com.gasagency.util.ApiResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @Value("${app.security.cookie.secure:true}")
    private boolean cookieSecure;

    @Value("${app.security.cookie.same-site:Lax}")
    private String cookieSameSite;

    @Value("${app.security.cookie.domain:}")
    private String cookieDomain;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthLoginResponseDTO>> login(@RequestBody AuthLoginRequestDTO loginRequest,
            HttpServletResponse response) {
        try {
            AuthService.LoginResult result = authService.login(loginRequest);

            ResponseCookie accessCookie = buildCookie("jwt_token", result.getAccessToken(), 4 * 60 * 60);
            response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

            ResponseCookie refreshCookie = buildCookie("refresh_token", result.getRefreshToken(), 7 * 24 * 60 * 60);
            response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

            return ResponseEntity.ok(ApiResponseUtil.success("Login successful", result.getUser()));
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<String>> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshTokenStr = null;
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("refresh_token".equals(cookie.getName())) {
                    refreshTokenStr = cookie.getValue();
                }
            }
        }
        if (refreshTokenStr == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponseUtil.error("No refresh token", "AUTH_REFRESH_MISSING"));
        }
        String newAccessToken;
        try {
            newAccessToken = authService.refreshAccessToken(refreshTokenStr);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponseUtil.error("Invalid or expired refresh token", "AUTH_REFRESH_INVALID"));
        }
        ResponseCookie accessCookie = buildCookie("jwt_token", newAccessToken, 4 * 60 * 60);
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        return ResponseEntity.ok().body(ApiResponseUtil.success("Access token refreshed"));
    }

    @PostMapping("/logout")
    @Transactional
    public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest request, HttpServletResponse response) {
        ResponseCookie jwtCookie = buildCookie("jwt_token", "", 0);
        response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());

        ResponseCookie refreshCookie = buildCookie("refresh_token", "", 0);
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        // Remove refresh token from DB
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("refresh_token".equals(cookie.getName())) {
                    authService.logout(cookie.getValue());
                }
            }
        }
        return ResponseEntity.ok(ApiResponseUtil.success("Logged out successfully"));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDTO>> register(@RequestBody AuthRegisterRequestDTO registerRequest) {
        UserDTO savedUser = authService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseUtil.success("User registered successfully", savedUser));
    }

    private ResponseCookie buildCookie(String name, String value, long maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite(cookieSameSite);

        if (StringUtils.hasText(cookieDomain)) {
            builder.domain(cookieDomain);
        }
        return builder.build();
    }

}

