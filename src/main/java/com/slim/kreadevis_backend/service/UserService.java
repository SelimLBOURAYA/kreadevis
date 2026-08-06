package com.slim.kreadevis_backend.service;

import com.slim.kreadevis_backend.dto.auth.RegisterRequest;
import com.slim.kreadevis_backend.dto.user.UserResponse;

import java.util.List;

public interface UserService {
    List<UserResponse> findAll();
    UserResponse findById(Long id);
    UserResponse findByLogin(String login);
    void delete(Long id);

    /** Admin-only: creates a new user with ROLE_ADMIN (public registration only ever grants ROLE_USER). */
    UserResponse createAdmin(RegisterRequest request);
}
