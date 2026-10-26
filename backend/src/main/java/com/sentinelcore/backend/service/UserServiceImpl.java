package com.sentinelcore.backend.service;

import com.sentinelcore.backend.dto.UserDTO;
import com.sentinelcore.backend.entity.Permission;
import com.sentinelcore.backend.entity.User;
import com.sentinelcore.backend.entity.Role;
import com.sentinelcore.backend.exception.BadRequestException;
import com.sentinelcore.backend.exception.ConflictException;
import com.sentinelcore.backend.exception.ResourceNotFoundException;
import com.sentinelcore.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Override
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream().map(this::mapToDTO).toList();
    }

    @Override
    public Page<UserDTO> getUsers(String search, String role, String status, Pageable pageable) {
        Specification<User> spec = Specification.where(null);

        if (search != null && !search.trim().isEmpty()) {
            String likePattern = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("fullName")), likePattern),
                    cb.like(cb.lower(root.get("username")), likePattern),
                    cb.like(cb.lower(root.get("email")), likePattern),
                    cb.like(cb.lower(root.get("department")), likePattern)
            ));
        }

        if (role != null && !role.trim().isEmpty()) {
            try {
                Role roleEnum = Role.valueOf(role.trim().toUpperCase());
                spec = spec.and((root, query, cb) -> cb.equal(root.get("role"), roleEnum));
            } catch (IllegalArgumentException e) {
                // Ignore invalid role filter
            }
        }

        if (status != null && !status.trim().isEmpty()) {
            boolean enabledVal = "active".equalsIgnoreCase(status) || "enabled".equalsIgnoreCase(status) || "true".equalsIgnoreCase(status);
            spec = spec.and((root, query, cb) -> cb.equal(root.get("enabled"), enabledVal));
        }

        return userRepository.findAll(spec, pageable).map(this::mapToDTO);
    }

    @Override
    public UserDTO getUserById(Long id) {
        return mapToDTO(findUser(id));
    }

    @Override
    @Transactional
    public UserDTO createUser(UserDTO dto) {
        User actor = currentActor();

        if (!StringUtils.hasText(dto.getUsername()) || !StringUtils.hasText(dto.getEmail())
                || !StringUtils.hasText(dto.getFullName())) {
            throw new BadRequestException("Full name, username and email are required");
        }
        validatePassword(dto.getPassword());

        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new ConflictException("Username already exists");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new ConflictException("Email already exists");
        }

        Role requestedRole = dto.getRole() != null ? dto.getRole() : Role.USER;
        if (requestedRole != Role.USER) {
            requirePermission(Permission.USER_ROLE_ASSIGN);
        }
        requireCanGrant(actor, requestedRole);

        User user = User.builder()
                .fullName(dto.getFullName())
                .username(dto.getUsername())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .role(requestedRole)
                .enabled(dto.getEnabled() != null ? dto.getEnabled() : true)
                .department(dto.getDepartment())
                .phoneNumber(dto.getPhoneNumber())
                .profilePicture(dto.getProfilePicture())
                .createdAt(LocalDateTime.now())
                // Non-super-admins can only create users inside their own tenant
                .tenantKey(actor.getRole() == Role.SUPER_ADMIN && dto.getTenantKey() != null
                        ? normalizeTenant(dto.getTenantKey()) : actor.getTenantKey())
                .build();

        userRepository.save(user);
        auditLogService.logSecurityEvent("USER_CREATED",
                "Created user " + user.getUsername() + " with role " + user.getRole());
        return mapToDTO(user);
    }

    @Override
    @Transactional
    public UserDTO updateUser(Long id, UserDTO dto) {
        User actor = currentActor();
        User user = findUser(id);
        requireCanManage(actor, user);

        if (StringUtils.hasText(dto.getFullName())) {
            user.setFullName(dto.getFullName());
        }
        if (StringUtils.hasText(dto.getEmail()) && !dto.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(dto.getEmail())) {
                throw new ConflictException("Email already exists");
            }
            user.setEmail(dto.getEmail());
        }

        if (dto.getRole() != null && dto.getRole() != user.getRole()) {
            requirePermission(Permission.USER_ROLE_ASSIGN);
            if (Objects.equals(actor.getId(), user.getId())) {
                throw new AccessDeniedException("You cannot change your own role");
            }
            requireCanGrant(actor, dto.getRole());
            if (user.getRole() == Role.SUPER_ADMIN) {
                requireAnotherActiveSuperAdmin(user);
            }
            auditLogService.logSecurityEvent("USER_ROLE_CHANGED",
                    "Changed role of " + user.getUsername() + " from " + user.getRole() + " to " + dto.getRole());
            user.setRole(dto.getRole());
        }

        if (dto.getEnabled() != null && !dto.getEnabled().equals(user.getEnabled())) {
            applyEnabled(actor, user, dto.getEnabled());
        }

        if (dto.getTenantKey() != null && actor.getRole() == Role.SUPER_ADMIN) {
            user.setTenantKey(normalizeTenant(dto.getTenantKey()));
        }

        user.setDepartment(dto.getDepartment());
        user.setPhoneNumber(dto.getPhoneNumber());
        if (dto.getProfilePicture() != null) {
            user.setProfilePicture(dto.getProfilePicture());
        }

        userRepository.save(user);
        return mapToDTO(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User actor = currentActor();
        User user = findUser(id);
        if (Objects.equals(actor.getId(), user.getId())) {
            throw new AccessDeniedException("You cannot delete your own account");
        }
        requireCanManage(actor, user);
        if (user.getRole() == Role.SUPER_ADMIN) {
            requireAnotherActiveSuperAdmin(user);
        }
        userRepository.delete(user);
        auditLogService.logSecurityEvent("USER_DELETED", "Deleted user " + user.getUsername());
    }

    @Override
    @Transactional
    public UserDTO toggleUserStatus(Long id) {
        User actor = currentActor();
        User user = findUser(id);
        requireCanManage(actor, user);
        applyEnabled(actor, user, !user.getEnabled());
        userRepository.save(user);
        return mapToDTO(user);
    }

    @Override
    @Transactional
    public UserDTO resetPassword(Long id, String newPassword) {
        User actor = currentActor();
        User user = findUser(id);
        requireCanManage(actor, user);
        validatePassword(newPassword);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        auditLogService.logSecurityEvent("USER_PASSWORD_RESET", "Password reset for " + user.getUsername());
        return mapToDTO(user);
    }

    @Override
    public UserDTO getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return mapToDTO(user);
    }

    @Override
    @Transactional
    public UserDTO updateProfile(String username, UserDTO dto) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Role, enabled flag and tenant are deliberately not editable through the profile endpoint
        user.setFullName(dto.getFullName());
        if (StringUtils.hasText(dto.getEmail()) && !dto.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(dto.getEmail())) {
                throw new ConflictException("Email already exists");
            }
            user.setEmail(dto.getEmail());
        }
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setDepartment(dto.getDepartment());
        if (dto.getProfilePicture() != null) {
            user.setProfilePicture(dto.getProfilePicture());
        }

        userRepository.save(user);
        return mapToDTO(user);
    }

    @Override
    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            auditLogService.logSecurityEvent("PASSWORD_CHANGE_FAILED", username, "Current password mismatch");
            throw new BadRequestException("Current password does not match");
        }
        validatePassword(newPassword);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        auditLogService.logSecurityEvent("PASSWORD_CHANGED", username, "Password changed by account owner");
    }

    private void applyEnabled(User actor, User user, boolean enabled) {
        if (Objects.equals(actor.getId(), user.getId()) && !enabled) {
            throw new AccessDeniedException("You cannot disable your own account");
        }
        if (!enabled && user.getRole() == Role.SUPER_ADMIN) {
            requireAnotherActiveSuperAdmin(user);
        }
        user.setEnabled(enabled);
        auditLogService.logSecurityEvent(enabled ? "USER_ENABLED" : "USER_DISABLED",
                (enabled ? "Enabled " : "Disabled ") + user.getUsername());
    }

    private void requireCanGrant(User actor, Role role) {
        if (role == Role.SUPER_ADMIN && actor.getRole() != Role.SUPER_ADMIN) {
            throw new AccessDeniedException("Only a SUPER_ADMIN can grant the SUPER_ADMIN role");
        }
    }

    private void requireCanManage(User actor, User target) {
        if (target.getRole() == Role.SUPER_ADMIN && actor.getRole() != Role.SUPER_ADMIN) {
            throw new AccessDeniedException("Only a SUPER_ADMIN can manage SUPER_ADMIN accounts");
        }
    }

    private void requireAnotherActiveSuperAdmin(User target) {
        boolean anotherExists = userRepository.findAll().stream()
                .anyMatch(u -> u.getRole() == Role.SUPER_ADMIN
                        && Boolean.TRUE.equals(u.getEnabled())
                        && !Objects.equals(u.getId(), target.getId()));
        if (!anotherExists) {
            throw new ConflictException("At least one active SUPER_ADMIN account must remain");
        }
    }

    private static void requirePermission(Permission permission) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean granted = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> permission.name().equals(a.getAuthority()));
        if (!granted) {
            throw new AccessDeniedException("Missing permission " + permission.name());
        }
    }

    private static void validatePassword(String password) {
        if (password == null || password.trim().length() < MIN_PASSWORD_LENGTH) {
            throw new BadRequestException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        if (password.length() > 128) {
            throw new BadRequestException("Password must be at most 128 characters");
        }
    }

    private static String normalizeTenant(String tenantKey) {
        String trimmed = tenantKey.trim().toLowerCase();
        if (!trimmed.matches("[a-z0-9][a-z0-9-]{0,63}")) {
            throw new BadRequestException("Tenant key must be lowercase alphanumeric with dashes");
        }
        return trimmed;
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    private User currentActor() {
        String username = AuditLogService.currentUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found"));
    }

    private UserDTO mapToDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.getEnabled())
                .department(user.getDepartment())
                .phoneNumber(user.getPhoneNumber())
                .profilePicture(user.getProfilePicture())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .tenantKey(user.getTenantKey())
                .build();
    }
}
