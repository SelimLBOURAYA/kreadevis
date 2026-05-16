package com.slim.kreadevis_backend.controller;

import com.slim.kreadevis_backend.dto.auth.AuthResponse;
import com.slim.kreadevis_backend.dto.auth.LoginRequest;
import com.slim.kreadevis_backend.dto.auth.RegisterRequest;
import com.slim.kreadevis_backend.entity.ERole;
import com.slim.kreadevis_backend.entity.Role;
import com.slim.kreadevis_backend.entity.User;
import com.slim.kreadevis_backend.repository.RoleRepository;
import com.slim.kreadevis_backend.repository.UserRepository;
import com.slim.kreadevis_backend.security.JwtUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = jwtUtils.generateToken(user.getLogin());
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.existsByLogin(request.login())) {
            throw new IllegalArgumentException("Login already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("Default role not found"));

        User user = new User();
        user.setLogin(request.login());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setAuthorities(Set.of(userRole));

        userRepository.save(user);

        String token = jwtUtils.generateToken(user.getLogin());
        return ResponseEntity.ok(new AuthResponse(token));
    }
}
