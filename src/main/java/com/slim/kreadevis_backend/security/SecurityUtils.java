package com.slim.kreadevis_backend.security;

import com.slim.kreadevis_backend.entity.ERole;
import com.slim.kreadevis_backend.entity.User;
import com.slim.kreadevis_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Reads the currently authenticated user off the security context.
 * Centralizes what used to be duplicated in each service (QuoteServiceImpl, ...).
 */
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        var userDetails = (UserDetailsImpl) auth.getPrincipal();
        return userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in DB"));
    }

    public boolean isAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(ERole.ROLE_ADMIN.name()));
    }

    /**
     * Runs the ownership-aware variant of a lookup: {@code adminLookup} for ROLE_ADMIN,
     * otherwise {@code scopedLookup} with the current user's id.
     * <p>
     * Single home for the admin-bypass branch — services state the two repository
     * calls and stay free of the conditional. Works for any return type
     * ({@code Optional<T>}, {@code Page<T>}, {@code List<T>}).
     */
    public <T> T resolveOwned(Supplier<T> adminLookup, Function<Long, T> scopedLookup) {
        return isAdmin() ? adminLookup.get() : scopedLookup.apply(getCurrentUser().getId());
    }
}
