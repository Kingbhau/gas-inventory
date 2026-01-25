
package com.gasagency.controller;

import com.gasagency.entity.User;
import com.gasagency.dto.UserDTO;
import com.gasagency.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserService userService;

    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping
    public ResponseEntity<UserDTO> createUser(@RequestBody UserDTO userDTO) {
        User createdUser = userService.createUser(userDTO);
        return new ResponseEntity<>(toDTO(createdUser), HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, @RequestBody User user) {
        Optional<User> updatedUser = userService.updateUser(id, user);
        if (updatedUser.isPresent()) {
            return new ResponseEntity<>(toDTO(updatedUser.get()), HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Boolean> softDeleteUser(@PathVariable Long id) {
        boolean deleted = userService.softDeleteUser(id);
        return new ResponseEntity<>(deleted, deleted ? HttpStatus.OK : HttpStatus.NOT_FOUND);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers().stream().map(this::toDTO).toList();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        Optional<User> user = userService.getUserById(id);
        if (user.isPresent()) {
            return new ResponseEntity<>(toDTO(user.get()), HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PostMapping("/{id}/change-password")
    public ResponseEntity<?> changePassword(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String currentPassword = payload.get("currentPassword");
        String newPassword = payload.get("newPassword");

        try {
            boolean changed = userService.changePassword(id, currentPassword, newPassword);
            if (changed) {
                return new ResponseEntity<>(new java.util.HashMap<String, Object>() {
                    {
                        put("success", true);
                        put("message", "Password changed successfully");
                    }
                }, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(new java.util.HashMap<String, Object>() {
                    {
                        put("success", false);
                        put("message", "Current password is incorrect");
                        put("error", "Invalid current password");
                    }
                }, HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(new java.util.HashMap<String, Object>() {
                {
                    put("success", false);
                    put("message", e.getMessage() != null ? e.getMessage() : "Failed to change password");
                    put("error", e.getClass().getSimpleName());
                }
            }, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private UserDTO toDTO(User user) {
        if (user == null)
            return null;
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setName(user.getName());
        dto.setMobileNo(user.getMobileNo());
        dto.setRole(user.getRole() != null ? user.getRole().name() : null);
        dto.setActive(user.isActive());
        if (user.getBusiness() != null) {
            dto.setBusinessId(user.getBusiness().getId());
        }
        return dto;
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping("/{id}/reactivate")
    public ResponseEntity<UserDTO> reactivateUser(@PathVariable Long id) {
        var userOpt = userService.reactivateUser(id);
        if (userOpt.isPresent()) {
            return new ResponseEntity<>(userOpt.get(), HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
}
