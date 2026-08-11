package com.sentinelcore.backend.service;

import com.sentinelcore.backend.dto.UserDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {

    List<UserDTO> getAllUsers();

    Page<UserDTO> getUsers(String search, String role, String status, Pageable pageable);

    UserDTO getUserById(Long id);

    UserDTO createUser(UserDTO userDTO);

    UserDTO updateUser(Long id, UserDTO userDTO);

    void deleteUser(Long id);

    UserDTO toggleUserStatus(Long id);

    UserDTO resetPassword(Long id, String newPassword);

    UserDTO getProfile(String username);

    UserDTO updateProfile(String username, UserDTO profileDTO);

    void changePassword(String username, String currentPassword, String newPassword);
}
