
package com.gasagency.controller;

import com.gasagency.entity.User;
import com.gasagency.repository.UserRepository;
import com.gasagency.util.JwtUtil;
import com.gasagency.entity.BusinessInfo;
import com.gasagency.repository.BusinessInfoRepository;
import com.gasagency.dto.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.gasagency.entity.RefreshToken;
import com.gasagency.repository.RefreshTokenRepository;
import java.time.Instant;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private BusinessInfoRepository businessInfoRepository;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> loginRequest, HttpServletResponse response) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.get("username"),
                            loginRequest.get("password")));
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            // Short-lived access token (e.g., 15 min)
            String accessToken = jwtUtil.generateToken(userDetails, 15 * 60 * 1000L);
            // Long-lived refresh token (e.g., 7 days)
            String refreshTokenStr = UUID.randomUUID().toString();
            Instant expiry = Instant.now().plusSeconds(7 * 24 * 60 * 60); // 7 days
            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setToken(refreshTokenStr);
            User user = userRepository.findByUsername(userDetails.getUsername()).get();
            refreshToken.setUser(user);
            refreshToken.setExpiryDate(expiry);
            refreshTokenRepository.save(refreshToken);

            // Set access token as HTTP-only cookie
            Cookie accessCookie = new Cookie("jwt_token", accessToken);
            accessCookie.setHttpOnly(true);
            accessCookie.setSecure(true); // Set to true in production (HTTPS)
            accessCookie.setPath("/");
            accessCookie.setMaxAge(15 * 60); // 15 min
            response.addCookie(accessCookie);

            // Set refresh token as HTTP-only cookie
            Cookie refreshCookie = new Cookie("refresh_token", refreshTokenStr);
            refreshCookie.setHttpOnly(true);
            refreshCookie.setSecure(true);
            refreshCookie.setPath("/");
            refreshCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
            response.addCookie(refreshCookie);

            Map<String, Object> resp = new HashMap<>();
            User loggedInUser = userRepository.findByUsername(userDetails.getUsername()).get();
            resp.put("id", loggedInUser.getId());
            resp.put("role", loggedInUser.getRole());
            resp.put("name", loggedInUser.getName());
            resp.put("username", loggedInUser.getUsername());
            resp.put("mobileNo", user.getMobileNo());
            if (user.getBusiness() != null) {
                resp.put("businessId", user.getBusiness().getId());
            } else {
                resp.put("businessId", null);
            }
            return resp;
        } catch (BadCredentialsException e) {
            throw new RuntimeException("Invalid username or password");
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshTokenStr = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refresh_token".equals(cookie.getName())) {
                    refreshTokenStr = cookie.getValue();
                }
            }
        }
        if (refreshTokenStr == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No refresh token");
        }
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenStr).orElse(null);
        if (refreshToken == null || refreshToken.getExpiryDate().isBefore(Instant.now())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired refresh token");
        }
        User user = refreshToken.getUser();
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(user.getUsername(),
                user.getPassword(),
                java.util.Collections
                        .singleton(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_" + user.getRole().name())));
        // Issue new access token
        String newAccessToken = jwtUtil.generateToken(userDetails, 15 * 60 * 1000L);
        Cookie accessCookie = new Cookie("jwt_token", newAccessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(15 * 60);
        response.addCookie(accessCookie);
        return ResponseEntity.ok().body("Access token refreshed");
    }

    @PostMapping("/logout")
    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        // Remove both cookies
        Cookie jwtCookie = new Cookie("jwt_token", null);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0);
        response.addCookie(jwtCookie);

        Cookie refreshCookie = new Cookie("refresh_token", null);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0);
        response.addCookie(refreshCookie);

        // Remove refresh token from DB
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refresh_token".equals(cookie.getName())) {
                    refreshTokenRepository.deleteByToken(cookie.getValue());
                }
            }
        }
    }

    @PostMapping("/register")
    public UserDTO register(@RequestBody Map<String, String> registerRequest) {
        if (!registerRequest.containsKey("businessId")) {
            throw new RuntimeException("businessId is required");
        }
        Long businessId = Long.parseLong(registerRequest.get("businessId"));
        BusinessInfo business = businessInfoRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Business not found"));
        User user = new User();
        user.setUsername(registerRequest.get("username"));
        user.setPassword(passwordEncoder.encode(registerRequest.get("password")));
        user.setRole(User.Role.valueOf(registerRequest.get("role")));
        user.setName(registerRequest.getOrDefault("name", ""));
        user.setMobileNo(registerRequest.getOrDefault("mobileNo", ""));
        user.setActive(true);
        user.setBusiness(business);
        User savedUser = userRepository.save(user);
        return convertToDTO(savedUser);
    }

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setName(user.getName());
        dto.setMobileNo(user.getMobileNo());
        dto.setRole(user.getRole().toString());
        dto.setActive(user.isActive());
        if (user.getBusiness() != null) {
            dto.setBusinessId(user.getBusiness().getId());
        }
        return dto;
    }

}
