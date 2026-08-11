package com.sentinelcore.backend.dto;

import com.sentinelcore.backend.entity.Role;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {

    private Long id;

    private String fullName;

    private String username;

    private String email;

    private String password; // Optional field for creating/updating users

    private Role role;

    private Boolean enabled;

    private String department;

    private String phoneNumber;

    private String profilePicture;

    private LocalDateTime lastLogin;

    private LocalDateTime createdAt;
}