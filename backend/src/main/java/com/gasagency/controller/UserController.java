
package com.gasagency.controller;

import com.gasagency.entity.User;
import com.gasagency.dto.UserDTO;
import com.gasagency.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
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
    public UserDTO createUser(@RequestBody UserDTO userDTO) {
        User createdUser = userService.createUser(userDTO);
        return toDTO(createdUser);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PutMapping("/{id}")
    public Optional<UserDTO> updateUser(@PathVariable Long id, @RequestBody User user) {
        return userService.updateUser(id, user).map(this::toDTO);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @DeleteMapping("/{id}")
    public boolean softDeleteUser(@PathVariable Long id) {
        return userService.softDeleteUser(id);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping
    public List<UserDTO> getAllActiveUsers() {
        return userService.getAllActiveUsers().stream().map(this::toDTO).toList();
    }

    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/{id}")
    public Optional<UserDTO> getUserById(@PathVariable Long id) {
        return userService.getUserById(id).map(this::toDTO);
    }

    @PostMapping("/{id}/change-password")
    public boolean changePassword(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String currentPassword = payload.get("currentPassword");
        String newPassword = payload.get("newPassword");
        return userService.changePassword(id, currentPassword, newPassword);
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
}
