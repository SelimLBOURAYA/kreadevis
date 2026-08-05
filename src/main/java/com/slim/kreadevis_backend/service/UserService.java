package com.slim.kreadevis_backend.service;

import com.slim.kreadevis_backend.dto.user.UserResponse;

import java.util.List;

public interface UserService {
    List<UserResponse> findAll();
    UserResponse findById(Long id);
    UserResponse findByLogin(String login);
    void delete(Long id);
}
