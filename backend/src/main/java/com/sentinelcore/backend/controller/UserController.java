package com.sentinelcore.backend.controller;

import com.sentinelcore.backend.dto.ApiResponse;
import com.sentinelcore.backend.dto.UserDTO;
import com.sentinelcore.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('USER_CREATE') || hasAuthority('USER_EDIT')")
    public ResponseEntity<ApiResponse<Page<UserDTO>>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "username") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<UserDTO> users = userService.getUsers(search, role, status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_CREATE') || hasAuthority('USER_EDIT')")
    public ResponseEntity<ApiResponse<UserDTO>> getUserById(@PathVariable Long id) {
        UserDTO user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", user));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ResponseEntity<ApiResponse<UserDTO>> createUser(@RequestBody UserDTO userDTO) {
        UserDTO saved = userService.createUser(userDTO);
        return ResponseEntity.ok(ApiResponse.success("User created successfully", saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_EDIT')")
    public ResponseEntity<ApiResponse<UserDTO>> updateUser(
            @PathVariable Long id,
            @RequestBody UserDTO userDTO) {
        UserDTO updatedUser = userService.updateUser(id, userDTO);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", updatedUser));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    public ResponseEntity<ApiResponse<Object>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }

    @PutMapping("/{id}/toggle-status")
    @PreAuthorize("hasAuthority('USER_STATUS_TOGGLE')")
    public ResponseEntity<ApiResponse<UserDTO>> toggleUserStatus(@PathVariable Long id) {
        UserDTO updated = userService.toggleUserStatus(id);
        return ResponseEntity.ok(ApiResponse.success("User status updated successfully", updated));
    }

    @PutMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('USER_PASSWORD_RESET')")
    public ResponseEntity<ApiResponse<UserDTO>> resetPassword(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> payload) {
        String newPassword = payload.get("newPassword");
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        UserDTO updated = userService.resetPassword(id, newPassword);
        return ResponseEntity.ok(ApiResponse.success("User password reset successfully", updated));
    }

    // PROFILE ENDPOINTS FOR LOGGED IN USERS
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserDTO>> getProfile() {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        UserDTO profile = userService.getProfile(currentUsername);
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", profile));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserDTO>> updateProfile(@RequestBody UserDTO profileDTO) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        UserDTO updated = userService.updateProfile(currentUsername, profileDTO);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updated));
    }

    @PutMapping("/profile/change-password")
    public ResponseEntity<ApiResponse<Object>> changePassword(
            @RequestBody java.util.Map<String, String> payload) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        String currentPassword = payload.get("currentPassword");
        String newPassword = payload.get("newPassword");
        userService.changePassword(currentUsername, currentPassword, newPassword);
        return ResponseEntity.ok(ApiResponse.success("Password updated successfully", null));
    }
}