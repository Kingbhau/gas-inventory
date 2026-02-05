
package com.gasagency.service;

import com.gasagency.entity.User;
import com.gasagency.repository.UserRepository;
import com.gasagency.repository.BusinessInfoRepository;
import com.gasagency.dto.UserDTO;
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
    public User createUser(UserDTO userDTO) {
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
        return userRepository.save(newUser);
    }

    public boolean changePassword(Long userId, String currentPassword, String newPassword) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(currentPassword, user.getPassword())) {
                user.setPassword(passwordEncoder.encode(newPassword));
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }

    @CacheEvict(value = { "usersActive", "usersAll" }, allEntries = true)
    public Optional<User> updateUser(Long id, User updatedUser) {
        return userRepository.findById(id).map(user -> {
            if (updatedUser.getUsername() != null && !updatedUser.getUsername().isEmpty()) {
                user.setUsername(updatedUser.getUsername());
            }
            if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
            }
            if (updatedUser.getRole() != null) {
                user.setRole(updatedUser.getRole());
            }
            if (updatedUser.getBusiness() != null) {
                user.setBusiness(updatedUser.getBusiness());
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
        });
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
    public List<User> getAllActiveUsers() {
        return userRepository.findByActiveTrue();
    }

    @Cacheable("usersAll")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id).filter(User::isActive);
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
        if (user.getBusiness() != null) {
            dto.setBusinessId(user.getBusiness().getId());
        }
        return dto;
    }
}
