package com.sentinelcore.backend.security.service;

import com.sentinelcore.backend.entity.User;
import com.sentinelcore.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Resolves the caller's identity and tenant from the trusted security context. Services must use
 * this instead of any user- or tenant-identifying value supplied in a request.
 */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    /** Tenant of every account whose tenant_key is null (all accounts created before multi-tenancy). */
    public static final String DEFAULT_TENANT = "default";

    private final UserRepository userRepository;

    public User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            throw new AccessDeniedException("Authentication required");
        }
        return userRepository.findByUsername(authentication.getName())
                .filter(user -> Boolean.TRUE.equals(user.getEnabled()))
                .orElseThrow(() -> new AccessDeniedException("Authenticated user is not active"));
    }

    public String currentUsername() {
        return currentUser().getUsername();
    }

    public String currentTenant() {
        return tenantOf(currentUser());
    }

    public static String tenantOf(User user) {
        return StringUtils.hasText(user.getTenantKey()) ? user.getTenantKey() : DEFAULT_TENANT;
    }
}
