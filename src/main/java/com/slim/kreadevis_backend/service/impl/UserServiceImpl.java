package com.slim.kreadevis_backend.service.impl;

import com.slim.kreadevis_backend.dto.auth.RegisterRequest;
import com.slim.kreadevis_backend.dto.user.UserResponse;
import com.slim.kreadevis_backend.entity.ERole;
import com.slim.kreadevis_backend.entity.Role;
import com.slim.kreadevis_backend.entity.User;
import com.slim.kreadevis_backend.mapper.UserMapper;
import com.slim.kreadevis_backend.repository.RoleRepository;
import com.slim.kreadevis_backend.repository.UserRepository;
import com.slim.kreadevis_backend.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    public List<UserResponse> findAll() {
        return userRepository.findAllByActiveTrue().stream().map(userMapper::toResponse).toList();
    }

    @Override
    public UserResponse findById(Long id) {
        return userMapper.toResponse(userRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id)));
    }

    @Override
    public UserResponse findByLogin(String login) {
        return userMapper.toResponse(userRepository.findByLoginAndActiveTrue(login)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + login)));
    }

    @Override
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
        user.setActive(false);
        userRepository.save(user);
    }

    @Override
    public UserResponse createAdmin(RegisterRequest request) {
        if (userRepository.existsByLoginAndActiveTrue(request.login())) {
            throw new IllegalArgumentException("Login already taken");
        }
        if (userRepository.existsByEmailAndActiveTrue(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                .orElseThrow(() -> new IllegalStateException("Admin role not found"));

        User user = new User();
        user.setLogin(request.login());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setAuthorities(Set.of(adminRole));

        return userMapper.toResponse(userRepository.save(user));
    }
}
