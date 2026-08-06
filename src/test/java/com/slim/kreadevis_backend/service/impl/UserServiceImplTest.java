package com.slim.kreadevis_backend.service.impl;

import com.slim.kreadevis_backend.dto.auth.RegisterRequest;
import com.slim.kreadevis_backend.dto.user.UserResponse;
import com.slim.kreadevis_backend.entity.ERole;
import com.slim.kreadevis_backend.entity.Role;
import com.slim.kreadevis_backend.entity.User;
import com.slim.kreadevis_backend.mapper.UserMapper;
import com.slim.kreadevis_backend.repository.RoleRepository;
import com.slim.kreadevis_backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserMapper userMapper;
    @InjectMocks private UserServiceImpl userService;

    private static final RegisterRequest REQUEST = new RegisterRequest("newadmin", "newadmin@test.com", "password123");

    @Test
    void createAdmin_shouldSaveUserWithAdminRole() {
        Role adminRole = new Role();
        adminRole.setName(ERole.ROLE_ADMIN);
        UserResponse response = new UserResponse(1L, "newadmin", "newadmin@test.com", java.util.Set.of("ROLE_ADMIN"));
        when(userRepository.existsByLoginAndActiveTrue("newadmin")).thenReturn(false);
        when(userRepository.existsByEmailAndActiveTrue("newadmin@test.com")).thenReturn(false);
        when(roleRepository.findByName(ERole.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toResponse(any())).thenReturn(response);

        UserResponse result = userService.createAdmin(REQUEST);

        assertThat(result).isEqualTo(response);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getAuthorities()).containsExactly(adminRole);
        assertThat(captor.getValue().getPassword()).isEqualTo("encoded");
    }

    @Test
    void createAdmin_shouldThrow_whenLoginTaken() {
        when(userRepository.existsByLoginAndActiveTrue("newadmin")).thenReturn(true);

        assertThatThrownBy(() -> userService.createAdmin(REQUEST))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Login already taken");
    }

    @Test
    void createAdmin_shouldThrow_whenEmailTaken() {
        when(userRepository.existsByLoginAndActiveTrue("newadmin")).thenReturn(false);
        when(userRepository.existsByEmailAndActiveTrue("newadmin@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createAdmin(REQUEST))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email already registered");
    }
}
