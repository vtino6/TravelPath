package com.travelpath.service;

import com.travelpath.dto.LoginRequest;
import com.travelpath.dto.UserRequest;
import com.travelpath.dto.UserResponse;
import com.travelpath.model.User;
import com.travelpath.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    public UserResponse registerUser(UserRequest request) {
        System.out.println("[UserService] Register attempt for email: " + request.getEmail());
        
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            System.out.println("[UserService] User already exists: " + request.getEmail());
            throw new RuntimeException("User with this email already exists");
        }
        
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new RuntimeException("Password is required");
        }
        String hashedPassword = passwordEncoder.encode(request.getPassword());
        user.setPassword(hashedPassword);
        System.out.println("[UserService] Password hashed successfully. Hash: " + hashedPassword.substring(0, Math.min(20, hashedPassword.length())) + "...");
        
        User saved = userRepository.save(user);
        System.out.println("[UserService] User registered successfully: " + saved.getEmail());
        
        return toResponse(saved);
    }
    
    public UserResponse loginUser(LoginRequest request) {
        System.out.println("[UserService] Login attempt for email: " + request.getEmail());
        
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> {
                System.out.println("[UserService] User not found: " + request.getEmail());
                return new RuntimeException("Invalid email or password");
            });
        
        System.out.println("[UserService] User found: " + user.getEmail());
        System.out.println("[UserService] Stored password hash: " + (user.getPassword() != null ? user.getPassword().substring(0, Math.min(20, user.getPassword().length())) + "..." : "null"));
        System.out.println("[UserService] Provided password length: " + (request.getPassword() != null ? request.getPassword().length() : 0));
        
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPassword());
        System.out.println("[UserService] Password matches: " + passwordMatches);
        
        if (!passwordMatches) {
            System.out.println("[UserService] Password verification failed");
            throw new RuntimeException("Invalid email or password");
        }
        
        System.out.println("[UserService] Login successful for: " + user.getEmail());
        return toResponse(user);
    }
    
    public UserResponse createOrUpdateUser(UserRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElse(new User());
        
        if (user.getId() == null) {
            user.setId(UUID.randomUUID().toString());
            if (request.getPassword() != null && !request.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(request.getPassword()));
            } else {
                throw new RuntimeException("Password is required for new users");
            }
        } else {
            if (request.getPassword() != null && !request.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(request.getPassword()));
            }
        }
        
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        
        User saved = userRepository.save(user);
        
        return toResponse(saved);
    }
    
    public UserResponse getUserById(String id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found: " + id));
        
        return toResponse(user);
    }
    
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        
        return toResponse(user);
    }
    
    public boolean userExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }
    
    public void resetPassword(String email, String newPassword) {
        System.out.println("[UserService] Resetting password for: " + email);
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found: " + email));
        
        String hashedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(hashedPassword);
        userRepository.save(user);
        System.out.println("[UserService] Password reset successfully for: " + email);
    }
    
    private UserResponse toResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
}

