package com.slim.kreadevis_backend.service.impl;

import com.slim.kreadevis_backend.config.JwtProperties;
import com.slim.kreadevis_backend.dto.auth.AuthResponse;
import com.slim.kreadevis_backend.dto.auth.LoginRequest;
import com.slim.kreadevis_backend.dto.auth.RefreshRequest;
import com.slim.kreadevis_backend.dto.auth.RegisterRequest;
import com.slim.kreadevis_backend.entity.ERole;
import com.slim.kreadevis_backend.entity.RefreshToken;
import com.slim.kreadevis_backend.entity.Role;
import com.slim.kreadevis_backend.entity.User;
import com.slim.kreadevis_backend.repository.RefreshTokenRepository;
import com.slim.kreadevis_backend.repository.RoleRepository;
import com.slim.kreadevis_backend.repository.UserRepository;
import com.slim.kreadevis_backend.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtils jwtUtils;

    private AuthServiceImpl authService;
    private User user;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties(
                "test-secret-key-that-is-at-least-64-characters-long-for-hs256!", 3_600_000L, 86_400_000L);
        authService = new AuthServiceImpl(userRepository, roleRepository, refreshTokenRepository,
                passwordEncoder, jwtUtils, jwtProperties);

        user = new User();
        user.setId(1L);
        user.setLogin("johndoe");
        user.setEmail("john@test.com");
        user.setPassword("hashed-password");
    }

    @Test
    void login_returnsTokens_whenCredentialsValid() {
        when(userRepository.findByEmailAndActiveTrue("john@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret1234567", "hashed-password")).thenReturn(true);
        when(jwtUtils.generateToken("john@test.com")).thenReturn("access-token");
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.login(new LoginRequest("john@test.com", "secret1234567"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isNotBlank();

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        assertThat(captor.getValue().isUsable()).isTrue();
    }

    @Test
    void login_throwsBadCredentials_whenUserNotFound() {
        when(userRepository.findByEmailAndActiveTrue("missing@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("missing@test.com", "whatever12345")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_throwsBadCredentials_whenPasswordWrong() {
        when(userRepository.findByEmailAndActiveTrue("john@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("john@test.com", "wrong-password")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void register_createsUserAndReturnsTokens() {
        when(userRepository.existsByLoginAndActiveTrue("janedoe")).thenReturn(false);
        when(userRepository.existsByEmailAndActiveTrue("jane@test.com")).thenReturn(false);
        Role userRole = new Role();
        userRole.setName(ERole.ROLE_USER);
        when(roleRepository.findByName(ERole.ROLE_USER)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(jwtUtils.generateToken("jane@test.com")).thenReturn("access-token");
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.register(new RegisterRequest("janedoe", "jane@test.com", "secret1234567"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isNotBlank();
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getAuthorities()).containsExactly(userRole);
    }

    @Test
    void register_throwsIllegalArgument_whenLoginTaken() {
        when(userRepository.existsByLoginAndActiveTrue("janedoe")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("janedoe", "jane@test.com", "secret1234567")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Login already taken");
    }

    @Test
    void register_throwsIllegalArgument_whenEmailTaken() {
        when(userRepository.existsByLoginAndActiveTrue("janedoe")).thenReturn(false);
        when(userRepository.existsByEmailAndActiveTrue("jane@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("janedoe", "jane@test.com", "secret1234567")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already registered");
    }

    @Test
    void register_throwsIllegalState_whenDefaultRoleMissing() {
        when(userRepository.existsByLoginAndActiveTrue("janedoe")).thenReturn(false);
        when(userRepository.existsByEmailAndActiveTrue("jane@test.com")).thenReturn(false);
        when(roleRepository.findByName(ERole.ROLE_USER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(new RegisterRequest("janedoe", "jane@test.com", "secret1234567")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void refresh_issuesNewTokens_andRevokesOldOne_whenValid() {
        when(jwtUtils.generateToken("john@test.com")).thenReturn("new-access-token");
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Capture the raw token issued by login, then present it back to refresh().
        when(userRepository.findByEmailAndActiveTrue("john@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        String rawRefreshToken = authService.login(new LoginRequest("john@test.com", "secret1234567")).refreshToken();

        ArgumentCaptor<RefreshToken> savedCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, atLeastOnce()).save(savedCaptor.capture());
        RefreshToken issued = savedCaptor.getAllValues().get(savedCaptor.getAllValues().size() - 1);
        when(refreshTokenRepository.findByTokenHash(issued.getTokenHash())).thenReturn(Optional.of(issued));

        AuthResponse response = authService.refresh(new RefreshRequest(rawRefreshToken));

        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isNotBlank().isNotEqualTo(rawRefreshToken);
        assertThat(issued.isRevoked()).isTrue();
    }

    @Test
    void refresh_throwsBadCredentials_whenTokenUnknown() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("unknown-token")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refresh_throwsBadCredentials_whenTokenRevoked() {
        RefreshToken revoked = RefreshToken.builder()
                .user(user)
                .tokenHash("irrelevant-because-mocked")
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(true)
                .build();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("some-token")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refresh_throwsBadCredentials_whenTokenExpired() {
        RefreshToken expired = RefreshToken.builder()
                .user(user)
                .tokenHash("irrelevant-because-mocked")
                .expiresAt(Instant.now().minusSeconds(1))
                .revoked(false)
                .build();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("some-token")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
