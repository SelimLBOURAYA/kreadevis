package com.slim.kreadevis_backend.security;

import com.slim.kreadevis_backend.entity.ERole;
import com.slim.kreadevis_backend.entity.Role;
import com.slim.kreadevis_backend.entity.User;
import com.slim.kreadevis_backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Direct tests for the single authorization decision point of lot 15.
 * Everywhere else {@code SecurityUtils} is mocked, so {@code resolveOwned} is
 * only exercised here.
 */
@ExtendWith(MockitoExtension.class)
class SecurityUtilsTest {

    private static final Long OWNER_ID = 42L;

    @Mock private UserRepository userRepository;
    @InjectMocks private SecurityUtils securityUtils;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @BeforeEach
    void resetContext() {
        SecurityContextHolder.clearContext();
    }

    // --- getCurrentUser ---

    @Test
    void getCurrentUser_shouldReturnUserFromRepository() {
        User user = authenticate(OWNER_ID, ERole.ROLE_USER);
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(user));

        assertThat(securityUtils.getCurrentUser()).isSameAs(user);
    }

    @Test
    void getCurrentUser_shouldThrow_whenAuthenticatedUserMissingFromDb() {
        authenticate(OWNER_ID, ERole.ROLE_USER);
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> securityUtils.getCurrentUser())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Authenticated user not found in DB");
    }

    // --- isAdmin ---

    @Test
    void isAdmin_shouldBeTrue_whenRoleAdminGranted() {
        authenticate(OWNER_ID, ERole.ROLE_ADMIN);

        assertThat(securityUtils.isAdmin()).isTrue();
    }

    @Test
    void isAdmin_shouldBeFalse_whenOnlyRoleUserGranted() {
        authenticate(OWNER_ID, ERole.ROLE_USER);

        assertThat(securityUtils.isAdmin()).isFalse();
    }

    // --- resolveOwned ---

    @Test
    void resolveOwned_shouldRunAdminLookup_andSkipScopedLookup_whenAdmin() {
        authenticate(OWNER_ID, ERole.ROLE_ADMIN);
        AtomicReference<Long> scopedCalledWith = new AtomicReference<>();

        String result = securityUtils.resolveOwned(
                () -> "all",
                ownerId -> {
                    scopedCalledWith.set(ownerId);
                    return "mine";
                });

        assertThat(result).isEqualTo("all");
        assertThat(scopedCalledWith.get()).isNull();
    }

    @Test
    void resolveOwned_shouldRunScopedLookupWithCurrentUserId_whenNotAdmin() {
        User user = authenticate(OWNER_ID, ERole.ROLE_USER);
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(user));
        AtomicReference<Boolean> adminCalled = new AtomicReference<>(false);

        String result = securityUtils.resolveOwned(
                () -> {
                    adminCalled.set(true);
                    return "all";
                },
                ownerId -> "mine:" + ownerId);

        assertThat(result).isEqualTo("mine:" + OWNER_ID);
        assertThat(adminCalled.get()).isFalse();
    }

    @Test
    void resolveOwned_shouldPropagateNullResult_whenLookupFindsNothing() {
        User user = authenticate(OWNER_ID, ERole.ROLE_USER);
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(user));

        Optional<String> result = securityUtils.resolveOwned(
                Optional::empty,
                ownerId -> Optional.empty());

        assertThat(result).isEmpty();
    }

    // --- helpers ---

    private User authenticate(Long userId, ERole role) {
        Role grantedRole = new Role();
        grantedRole.setName(role);
        User user = new User();
        user.setId(userId);
        user.setLogin("user" + userId);
        user.setEmail("user" + userId + "@test.com");
        user.setPassword("hashed");
        user.setAuthorities(Set.of(grantedRole));

        UserDetailsImpl principal = new UserDetailsImpl(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        return user;
    }
}
