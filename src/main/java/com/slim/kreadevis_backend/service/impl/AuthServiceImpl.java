package com.slim.kreadevis_backend.service.impl;

import com.slim.kreadevis_backend.dto.auth.AuthResponse;
import com.slim.kreadevis_backend.dto.auth.LoginRequest;
import com.slim.kreadevis_backend.dto.auth.RegisterRequest;
import com.slim.kreadevis_backend.entity.ERole;
import com.slim.kreadevis_backend.entity.Role;
import com.slim.kreadevis_backend.entity.User;
import com.slim.kreadevis_backend.repository.RoleRepository;
import com.slim.kreadevis_backend.repository.UserRepository;
import com.slim.kreadevis_backend.security.JwtUtils;
import com.slim.kreadevis_backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailAndActiveTrue(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        return new AuthResponse(jwtUtils.generateToken(user.getLogin()));
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByLoginAndActiveTrue(request.login())) {
            throw new IllegalArgumentException("Login already taken");
        }
        if (userRepository.existsByEmailAndActiveTrue(request.email())) {
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

        return new AuthResponse(jwtUtils.generateToken(user.getLogin()));
    }
}
