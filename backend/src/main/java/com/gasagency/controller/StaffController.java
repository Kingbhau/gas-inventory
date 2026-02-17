package com.gasagency.controller;

import com.gasagency.dto.request.ChangePasswordRequestDTO;
import com.gasagency.dto.response.SimpleStatusDTO;
import com.gasagency.service.UserService;
import com.gasagency.util.ApiResponse;
import com.gasagency.util.ApiResponseUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff")
public class StaffController {
    private final UserService userService;

    public StaffController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<SimpleStatusDTO>> changeOwnPassword(
            @RequestBody ChangePasswordRequestDTO payload,
            Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponseUtil.error("Unauthorized", "UNAUTHORIZED"));
        }

        String currentPassword = payload.getCurrentPassword();
        String newPassword = payload.getNewPassword();

        try {
            boolean changed = userService.changePasswordByUsername(authentication.getName(), currentPassword, newPassword);
            if (changed) {
                return ResponseEntity.ok(ApiResponseUtil.success("Password changed successfully",
                        new SimpleStatusDTO("SUCCESS")));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponseUtil.error("Current password is incorrect", "INVALID_CREDENTIALS"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseUtil.error(
                            e.getMessage() != null ? e.getMessage() : "Failed to change password",
                            "PASSWORD_CHANGE_FAILED"));
        }
    }
}
