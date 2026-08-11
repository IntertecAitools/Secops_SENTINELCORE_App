package com.sentinelcore.backend.service;

import com.sentinelcore.backend.dto.UserDTO;
import com.sentinelcore.backend.entity.User;
import com.sentinelcore.backend.entity.Role;
import com.sentinelcore.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
        User user = userRepository.findById(id).orElseThrow();
        return mapToDTO(user);
    }

    @Override
    public UserDTO createUser(UserDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .fullName(dto.getFullName())
                .username(dto.getUsername())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .role(dto.getRole() != null ? dto.getRole() : Role.USER)
                .enabled(dto.getEnabled() != null ? dto.getEnabled() : true)
                .department(dto.getDepartment())
                .phoneNumber(dto.getPhoneNumber())
                .profilePicture(dto.getProfilePicture())
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);
        return mapToDTO(user);
    }

    @Override
    public UserDTO updateUser(Long id, UserDTO dto) {
        User user = userRepository.findById(id).orElseThrow();

        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setRole(dto.getRole());
        user.setEnabled(dto.getEnabled());
        user.setDepartment(dto.getDepartment());
        user.setPhoneNumber(dto.getPhoneNumber());
        if (dto.getProfilePicture() != null) {
            user.setProfilePicture(dto.getProfilePicture());
        }

        userRepository.save(user);
        return mapToDTO(user);
    }

    @Override
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    public UserDTO toggleUserStatus(Long id) {
        User user = userRepository.findById(id).orElseThrow();
        user.setEnabled(!user.getEnabled());
        userRepository.save(user);
        return mapToDTO(user);
    }

    @Override
    public UserDTO resetPassword(Long id, String newPassword) {
        User user = userRepository.findById(id).orElseThrow();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return mapToDTO(user);
    }

    @Override
    public UserDTO getProfile(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return mapToDTO(user);
    }

    @Override
    public UserDTO updateProfile(String username, UserDTO dto) {
        User user = userRepository.findByUsername(username).orElseThrow();

        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setDepartment(dto.getDepartment());
        if (dto.getProfilePicture() != null) {
            user.setProfilePicture(dto.getProfilePicture());
        }

        userRepository.save(user);
        return mapToDTO(user);
    }

    @Override
    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username).orElseThrow();
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Current password does not match");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
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
                .build();
    }
}