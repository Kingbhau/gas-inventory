
package com.gasagency.service;

import com.gasagency.entity.User;
import com.gasagency.repository.UserRepository;
import com.gasagency.repository.BusinessInfoRepository;
import com.gasagency.dto.response.UserDTO;
import com.gasagency.dto.request.UserUpdateRequestDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private BusinessInfoRepository businessInfoRepository;

    @CacheEvict(value = { "usersActive", "usersAll" }, allEntries = true)
    public UserDTO createUser(UserDTO userDTO) {
        User newUser = new User();
        newUser.setUsername(userDTO.getUsername());
        // Set default password if not provided
        String defaultPassword = "user@123";
        String rawPassword = defaultPassword;
        newUser.setPassword(passwordEncoder.encode(rawPassword));
        if (userDTO.getRole() != null) {
            try {
                newUser.setRole(User.Role.valueOf(userDTO.getRole()));
            } catch (Exception e) {
                newUser.setRole(User.Role.STAFF);
            }
        } else {
            newUser.setRole(User.Role.STAFF);
        }
        if (userDTO.getBusinessId() != null) {
            businessInfoRepository.findById(userDTO.getBusinessId()).ifPresent(newUser::setBusiness);
        }
        newUser.setName(userDTO.getName());
        newUser.setMobileNo(userDTO.getMobileNo());
        newUser.setActive(true);
        User saved = userRepository.save(newUser);
        return convertToDTO(saved);
    }

    public boolean changePassword(Long userId, String currentPassword, String newPassword) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (!user.isActive()) {
                return false;
            }
            if (passwordEncoder.matches(currentPassword, user.getPassword())) {
                user.setPassword(passwordEncoder.encode(newPassword));
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }

    public boolean changePasswordByUsername(String username, String currentPassword, String newPassword) {
        if (username == null || username.isBlank()) {
            return false;
        }
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (!user.isActive()) {
                return false;
            }
            if (passwordEncoder.matches(currentPassword, user.getPassword())) {
                user.setPassword(passwordEncoder.encode(newPassword));
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }

    @CacheEvict(value = { "usersActive", "usersAll" }, allEntries = true)
    public Optional<UserDTO> updateUser(Long id, UserUpdateRequestDTO updatedUser) {
        return userRepository.findById(id).map(user -> {
            if (updatedUser.getUsername() != null && !updatedUser.getUsername().isEmpty()) {
                user.setUsername(updatedUser.getUsername());
            }
            if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
            }
            if (updatedUser.getRole() != null) {
                try {
                    user.setRole(User.Role.valueOf(updatedUser.getRole()));
                } catch (Exception e) {
                    // Ignore invalid role updates
                }
            }
            if (updatedUser.getBusinessId() != null) {
                businessInfoRepository.findById(updatedUser.getBusinessId())
                        .ifPresent(user::setBusiness);
            }
            if (updatedUser.getName() != null && !updatedUser.getName().isEmpty()) {
                user.setName(updatedUser.getName());
            }
            if (updatedUser.getMobileNo() != null && !updatedUser.getMobileNo().isEmpty()) {
                user.setMobileNo(updatedUser.getMobileNo());
            }
            if (updatedUser.getActive() != null) {
                user.setActive(updatedUser.getActive());
            }
            return userRepository.save(user);
        }).map(this::convertToDTO);
    }

    @CacheEvict(value = { "usersActive", "usersAll" }, allEntries = true)
    public boolean softDeleteUser(Long id) {
        return userRepository.findById(id).map(user -> {
            user.setActive(false);
            userRepository.save(user);
            return true;
        }).orElse(false);
    }

    @Cacheable("usersActive")
    public List<UserDTO> getAllActiveUsers() {
        return userRepository.findByActiveTrue().stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Cacheable("usersAll")
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDTO)
                .toList();
    }

    public Optional<UserDTO> getUserById(Long id) {
        return userRepository.findById(id)
                .filter(User::isActive)
                .map(this::convertToDTO);
    }

    @CacheEvict(value = { "usersActive", "usersAll" }, allEntries = true)
    public Optional<UserDTO> reactivateUser(Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        User user = userOpt.get();
        if (user.isActive()) {
            throw new IllegalArgumentException("User is already active");
        }

        user.setActive(true);
        User saved = userRepository.save(user);
        return Optional.of(convertToDTO(saved));
    }

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setName(user.getName());
        dto.setMobileNo(user.getMobileNo());
        dto.setRole(user.getRole().toString());
        dto.setActive(user.isActive());
        dto.setCreatedBy(user.getCreatedBy());
        dto.setCreatedDate(user.getCreatedDate());
        dto.setUpdatedBy(user.getUpdatedBy());
        dto.setUpdatedDate(user.getUpdatedDate());
        if (user.getBusiness() != null) {
            dto.setBusinessId(user.getBusiness().getId());
        }
        return dto;
    }
}

