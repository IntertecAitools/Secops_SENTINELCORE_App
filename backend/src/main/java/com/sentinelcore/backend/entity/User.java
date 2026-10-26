package com.sentinelcore.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column
    private String department;

    @Column
    private String phoneNumber;

    @Column(columnDefinition = "TEXT")
    private String profilePicture;

    @Column
    private LocalDateTime lastLogin;

    @Column
    private LocalDateTime createdAt;

    /**
     * Tenant this user belongs to. Null means the single built-in tenant ("default"), which keeps
     * every pre-existing account working without a data migration. Only SUPER_ADMIN can change it.
     */
    @Column(name = "tenant_key", length = 64)
    private String tenantKey;
}