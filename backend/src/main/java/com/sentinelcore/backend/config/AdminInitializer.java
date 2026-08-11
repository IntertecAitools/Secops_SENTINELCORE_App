package com.sentinelcore.backend.config;

import com.sentinelcore.backend.entity.Role;
import com.sentinelcore.backend.entity.User;
import com.sentinelcore.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            // Drop legacy enum check constraint to allow new RBAC roles in PostgreSQL
            jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;");
            System.out.println("Constraint 'users_role_check' dropped successfully or did not exist.");
        } catch (Exception e) {
            System.err.println("Failed to drop users_role_check constraint: " + e.getMessage());
        }

        seedUser("superadmin", "superadmin123", "System Super Admin", Role.SUPER_ADMIN, "IT Security");
        seedUser("admin", "admin123", "System Administrator", Role.ADMIN, "IT Operations");
        seedUser("analyst", "analyst123", "Security Analyst One", Role.SECURITY_ANALYST, "SOC Analyst Team");
        seedUser("incident", "incident123", "Incident Manager One", Role.INCIDENT_MANAGER, "Incident Response Team");
        seedUser("vulnerability", "vulnerability123", "Vulnerability Specialist", Role.VULNERABILITY_MANAGER, "SecOps Engineering");
        seedUser("asset", "asset123", "Asset Manager One", Role.ASSET_MANAGER, "IT Infrastructure");
        seedUser("auditor", "auditor123", "External Platform Auditor", Role.AUDITOR, "Compliance & Auditing");
        seedUser("user", "user123", "Standard Employee", Role.USER, "General Department");
    }

    private void seedUser(String username, String rawPassword, String fullName, Role role, String department) {
        User user = userRepository.findByUsername(username).orElseGet(() ->
                User.builder()
                        .username(username)
                        .email(username + "@sentinelcore.com")
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        user.setFullName(fullName);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setDepartment(department);
        if (user.getPhoneNumber() == null) {
            user.setPhoneNumber("+1 (555) 000-" + (role.ordinal() * 1111));
        }
        user.setEnabled(true);

        userRepository.save(user);

        System.out.println("==================================");
        System.out.println("Default Account Synchronized");
        System.out.println("Username : " + username);
        System.out.println("Role     : " + role.name());
        System.out.println("==================================");
    }
}