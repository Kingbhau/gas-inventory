
package com.gasagency.controller;

import com.gasagency.dto.request.ChangePasswordRequestDTO;
import com.gasagency.dto.response.UserDTO;
import com.gasagency.dto.response.SimpleStatusDTO;
import com.gasagency.dto.request.UserUpdateRequestDTO;
import com.gasagency.service.UserService;
import com.gasagency.util.ApiResponse;
import com.gasagency.util.ApiResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserService userService;

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @PostMapping
    public ResponseEntity<ApiResponse<UserDTO>> createUser(@RequestBody UserDTO userDTO) {
        // Get current user's role
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isOwner = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_OWNER"));

        // Manager cannot create Owner role users
        if (!isOwner && "OWNER".equals(userDTO.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponseUtil.error("Manager cannot create Owner users", "FORBIDDEN_OPERATION"));
        }

        UserDTO createdUser = userService.createUser(userDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseUtil.success("User created successfully", createdUser));
    }

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDTO>> updateUser(@PathVariable Long id,
            @RequestBody UserUpdateRequestDTO user) {
        Optional<UserDTO> updatedUser = userService.updateUser(id, user);
        if (updatedUser.isPresent()) {
            return ResponseEntity.ok(ApiResponseUtil.success("User updated successfully", updatedUser.get()));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseUtil.error("User not found", "RESOURCE_NOT_FOUND"));
    }

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Boolean>> softDeleteUser(@PathVariable Long id) {
        boolean deleted = userService.softDeleteUser(id);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseUtil.error("User not found", "RESOURCE_NOT_FOUND"));
        }
        return ResponseEntity.ok(ApiResponseUtil.success("User deleted successfully", true));
    }

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserDTO>>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponseUtil.success("Users retrieved successfully", users));
    }

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDTO>> getUserById(@PathVariable Long id) {
        Optional<UserDTO> user = userService.getUserById(id);
        if (user.isPresent()) {
            return ResponseEntity.ok(ApiResponseUtil.success("User retrieved successfully", user.get()));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseUtil.error("User not found", "RESOURCE_NOT_FOUND"));
    }

    @PostMapping("/{id}/change-password")
    public ResponseEntity<ApiResponse<SimpleStatusDTO>> changePassword(@PathVariable Long id,
            @RequestBody ChangePasswordRequestDTO payload) {
        String currentPassword = payload.getCurrentPassword();
        String newPassword = payload.getNewPassword();

        try {
            boolean changed = userService.changePassword(id, currentPassword, newPassword);
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

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @PostMapping("/{id}/reactivate")
    public ResponseEntity<ApiResponse<UserDTO>> reactivateUser(@PathVariable Long id) {
        var userOpt = userService.reactivateUser(id);
        if (userOpt.isPresent()) {
            return ResponseEntity.ok(ApiResponseUtil.success("User reactivated successfully", userOpt.get()));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseUtil.error("User not found", "RESOURCE_NOT_FOUND"));
    }
}

