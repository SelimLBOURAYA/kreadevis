package com.slim.kreadevis_backend.controller;

import com.slim.kreadevis_backend.dto.auth.RegisterRequest;
import com.slim.kreadevis_backend.dto.user.UserResponse;
import com.slim.kreadevis_backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Admin-only user management. Public registration ({@link AuthController}) only ever grants ROLE_USER;
 * creating another admin goes through here.
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> createAdmin(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = userService.createAdmin(request);
        return ResponseEntity.created(URI.create("/api/users/" + response.id())).body(response);
    }
}
