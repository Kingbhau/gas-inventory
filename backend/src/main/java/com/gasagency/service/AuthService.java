package com.gasagency.service;

import com.gasagency.dto.request.AuthLoginRequestDTO;
import com.gasagency.dto.request.AuthRegisterRequestDTO;
import com.gasagency.dto.response.AuthLoginResponseDTO;
import com.gasagency.dto.response.UserDTO;
import com.gasagency.entity.BusinessInfo;
import com.gasagency.entity.RefreshToken;
import com.gasagency.entity.User;
import com.gasagency.repository.BusinessInfoRepository;
import com.gasagency.repository.RefreshTokenRepository;
import com.gasagency.repository.UserRepository;
import com.gasagency.util.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {
    public static class LoginResult {
        private final String accessToken;
        private final String refreshToken;
        private final AuthLoginResponseDTO user;

        public LoginResult(String accessToken, String refreshToken, AuthLoginResponseDTO user) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.user = user;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public AuthLoginResponseDTO getUser() {
            return user;
        }
    }

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final BusinessInfoRepository businessInfoRepository;

    public AuthService(AuthenticationManager authenticationManager,
            JwtUtil jwtUtil,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            RefreshTokenRepository refreshTokenRepository,
            BusinessInfoRepository businessInfoRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.businessInfoRepository = businessInfoRepository;
    }

    public LoginResult login(AuthLoginRequestDTO loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()));
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            String accessToken = jwtUtil.generateToken(userDetails, 4 * 60 * 60 * 1000L);
            String refreshTokenStr = UUID.randomUUID().toString();
            Instant expiry = Instant.now().plusSeconds(7 * 24 * 60 * 60);

            User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow(
                    () -> new RuntimeException("User not found"));

            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setToken(refreshTokenStr);
            refreshToken.setUser(user);
            refreshToken.setExpiryDate(expiry);
            refreshTokenRepository.save(refreshToken);

            return new LoginResult(accessToken, refreshTokenStr, toLoginResponse(user));
        } catch (BadCredentialsException e) {
            throw new RuntimeException("Invalid username or password");
        }
    }

    public String refreshAccessToken(String refreshTokenStr) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenStr).orElse(null);
        if (refreshToken == null || refreshToken.getExpiryDate().isBefore(Instant.now())) {
            throw new RuntimeException("Invalid or expired refresh token");
        }
        User user = refreshToken.getUser();
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(user.getUsername(),
                user.getPassword(),
                java.util.Collections
                        .singleton(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_" + user.getRole().name())));
        return jwtUtil.generateToken(userDetails, 4 * 60 * 60 * 1000L);
    }

    @Transactional
    public void logout(String refreshTokenStr) {
        if (refreshTokenStr == null || refreshTokenStr.isBlank()) {
            return;
        }
        refreshTokenRepository.deleteByToken(refreshTokenStr);
    }

    public UserDTO register(AuthRegisterRequestDTO registerRequest) {
        if (registerRequest.getBusinessId() == null) {
            throw new RuntimeException("businessId is required");
        }
        Long businessId = registerRequest.getBusinessId();
        BusinessInfo business = businessInfoRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Business not found"));
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setRole(User.Role.valueOf(registerRequest.getRole()));
        user.setName(registerRequest.getName() != null ? registerRequest.getName() : "");
        user.setMobileNo(registerRequest.getMobileNo() != null ? registerRequest.getMobileNo() : "");
        user.setActive(true);
        user.setBusiness(business);
        User savedUser = userRepository.save(user);
        return toUserDTO(savedUser);
    }

    private AuthLoginResponseDTO toLoginResponse(User user) {
        AuthLoginResponseDTO resp = new AuthLoginResponseDTO();
        resp.setId(user.getId());
        resp.setRole(user.getRole().toString());
        resp.setName(user.getName());
        resp.setUsername(user.getUsername());
        resp.setMobileNo(user.getMobileNo());
        if (user.getBusiness() != null) {
            resp.setBusinessId(user.getBusiness().getId());
        }
        return resp;
    }

    private UserDTO toUserDTO(User user) {
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
