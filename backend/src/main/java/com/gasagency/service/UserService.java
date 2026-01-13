
package com.gasagency.service;

import com.gasagency.entity.User;
import com.gasagency.repository.UserRepository;
import com.gasagency.repository.BusinessInfoRepository;
import com.gasagency.dto.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

    public Optional<User> updateUser(Long id, User updatedUser) {
        return userRepository.findById(id).map(user -> {
            if (updatedUser.getUsername() != null && !updatedUser.getUsername().isEmpty()) {
                user.setUsername(updatedUser.getUsername());
            }
            if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
                user.setPassword(updatedUser.getPassword());
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

    public boolean softDeleteUser(Long id) {
        return userRepository.findById(id).map(user -> {
            user.setActive(false);
            userRepository.save(user);
            return true;
        }).orElse(false);
    }

    public List<User> getAllActiveUsers() {
        return userRepository.findByActiveTrue();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id).filter(User::isActive);
    }
}
