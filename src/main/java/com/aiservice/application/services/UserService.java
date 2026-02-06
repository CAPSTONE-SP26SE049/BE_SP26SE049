package com.aiservice.application.services;

import com.aiservice.domain.entities.User;
import com.aiservice.presentation.dto.UserCreateRequest;
import com.aiservice.presentation.dto.UserResponse;
import com.aiservice.presentation.dto.UserUpdateRequest;

import java.util.List;

public interface UserService {
    
    /**
     * Create a new user
     * @param request user creation request
     * @return created user response
     */
    UserResponse createUser(UserCreateRequest request);
    
    /**
     * Authenticate user with username and password
     * @param username user's username
     * @param password user's password
     * @return authenticated user
     */
    User authenticateUser(String username, String password);
    
    /**
     * Get user by ID
     * @param id user ID
     * @return user response
     */
    UserResponse getUserById(Long id);
    
    /**
     * Get user by username
     * @param username user's username
     * @return user response
     */
    UserResponse getUserByUsername(String username);
    
    /**
     * Update user
     * @param id user ID
     * @param request update request
     * @return updated user response
     */
    UserResponse updateUser(Long id, UserUpdateRequest request);
    
    /**
     * Delete user
     * @param id user ID
     */
    void deleteUser(Long id);
    
    /**
     * Get all users
     * @return list of user responses
     */
    List<UserResponse> getAllUsers();
}
