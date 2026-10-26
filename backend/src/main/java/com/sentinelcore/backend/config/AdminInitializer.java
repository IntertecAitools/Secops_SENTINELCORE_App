package com.sentinelcore.backend.config;

import com.sentinelcore.backend.entity.Role;
import com.sentinelcore.backend.entity.User;
import com.sentinelcore.backend.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Account bootstrap.
 * <ul>
 *   <li>Existing accounts are never modified: passwords, roles and status are left untouched.</li>
 *   <li>A SUPER_ADMIN can be provisioned from APP_BOOTSTRAP_ADMIN_USERNAME / APP_BOOTSTRAP_ADMIN_PASSWORD.</li>
 *   <li>Demo accounts with well-known passwords are created only when APP_DEMO_USERS_ENABLED=true
 *       (local development only).</li>
 * </ul>
 */
@Component
@Slf4j
public class AdminInitializer implements CommandLineRunner {

    // Development-only demo accounts. Created only when explicitly enabled; never re-seeded over existing users.
    private static final Map<String, DemoAccount> DEMO_ACCOUNTS = new LinkedHashMap<>();

    static {
        DEMO_ACCOUNTS.put("superadmin", new DemoAccount("superadmin123", "System Super Admin", Role.SUPER_ADMIN, "IT Security"));
        DEMO_ACCOUNTS.put("admin", new DemoAccount("admin123", "System Administrator", Role.ADMIN, "IT Operations"));
        DEMO_ACCOUNTS.put("analyst", new DemoAccount("analyst123", "Security Analyst One", Role.SECURITY_ANALYST, "SOC Analyst Team"));
        DEMO_ACCOUNTS.put("incident", new DemoAccount("incident123", "Incident Manager One", Role.INCIDENT_MANAGER, "Incident Response Team"));
        DEMO_ACCOUNTS.put("vulnerability", new DemoAccount("vulnerability123", "Vulnerability Specialist", Role.VULNERABILITY_MANAGER, "SecOps Engineering"));
        DEMO_ACCOUNTS.put("asset", new DemoAccount("asset123", "Asset Manager One", Role.ASSET_MANAGER, "IT Infrastructure"));
        DEMO_ACCOUNTS.put("auditor", new DemoAccount("auditor123", "External Platform Auditor", Role.AUDITOR, "Compliance & Auditing"));
        DEMO_ACCOUNTS.put("user", new DemoAccount("user123", "Standard Employee", Role.USER, "General Department"));
    }

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final boolean demoUsersEnabled;
    private final String bootstrapAdminUsername;
    private final String bootstrapAdminPassword;
    private final String bootstrapAdminEmail;

    public AdminInitializer(UserRepository userRepository,
                            PasswordEncoder passwordEncoder,
                            JdbcTemplate jdbcTemplate,
                            @Value("${app.bootstrap.demo-users-enabled:false}") boolean demoUsersEnabled,
                            @Value("${app.bootstrap.admin.username:}") String bootstrapAdminUsername,
                            @Value("${app.bootstrap.admin.password:}") String bootstrapAdminPassword,
                            @Value("${app.bootstrap.admin.email:}") String bootstrapAdminEmail) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
        this.demoUsersEnabled = demoUsersEnabled;
        this.bootstrapAdminUsername = bootstrapAdminUsername;
        this.bootstrapAdminPassword = bootstrapAdminPassword;
        this.bootstrapAdminEmail = bootstrapAdminEmail;
    }

    @Override
    public void run(String... args) {
        try {
            // Drop legacy enum check constraint to allow new RBAC roles in PostgreSQL (idempotent, pre-existing behaviour)
            jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check");
        } catch (Exception e) {
            log.warn("Could not drop users_role_check constraint: {}", e.getMessage());
        }

        provisionBootstrapAdmin();

        if (demoUsersEnabled) {
            log.warn("APP_DEMO_USERS_ENABLED=true: creating missing demo accounts with well-known passwords. "
                    + "Never enable this outside local development.");
            DEMO_ACCOUNTS.forEach(this::createDemoUserIfMissing);
        } else {
            warnAboutDefaultPasswords();
        }

        if (userRepository.count() == 0) {
            log.warn("No user accounts exist. Set APP_BOOTSTRAP_ADMIN_USERNAME and APP_BOOTSTRAP_ADMIN_PASSWORD "
                    + "to provision the first SUPER_ADMIN (or APP_DEMO_USERS_ENABLED=true for local development).");
        }
    }

    private void provisionBootstrapAdmin() {
        if (!StringUtils.hasText(bootstrapAdminUsername) || !StringUtils.hasText(bootstrapAdminPassword)) {
            return;
        }
        if (bootstrapAdminPassword.length() < 12) {
            throw new IllegalStateException("APP_BOOTSTRAP_ADMIN_PASSWORD must be at least 12 characters");
        }
        if (userRepository.existsByUsername(bootstrapAdminUsername)) {
            log.info("Bootstrap admin '{}' already exists; leaving it unchanged", bootstrapAdminUsername);
            return;
        }
        String email = StringUtils.hasText(bootstrapAdminEmail)
                ? bootstrapAdminEmail : bootstrapAdminUsername + "@sentinelcore.local";
        userRepository.save(User.builder()
                .username(bootstrapAdminUsername)
                .email(email)
                .fullName("Bootstrap Administrator")
                .password(passwordEncoder.encode(bootstrapAdminPassword))
                .role(Role.SUPER_ADMIN)
                .department("IT Security")
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build());
        log.info("Provisioned bootstrap SUPER_ADMIN '{}'", bootstrapAdminUsername);
    }

    private void createDemoUserIfMissing(String username, DemoAccount account) {
        if (userRepository.findByUsername(username).isPresent()) {
            return;
        }
        userRepository.save(User.builder()
                .username(username)
                .email(username + "@sentinelcore.com")
                .fullName(account.fullName())
                .password(passwordEncoder.encode(account.password()))
                .role(account.role())
                .department(account.department())
                .phoneNumber("+1 (555) 000-" + (account.role().ordinal() * 1111))
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build());
        log.info("Created demo account '{}' ({})", username, account.role());
    }

    private void warnAboutDefaultPasswords() {
        DEMO_ACCOUNTS.forEach((username, account) -> userRepository.findByUsername(username)
                .filter(user -> Boolean.TRUE.equals(user.getEnabled()))
                .filter(user -> passwordEncoder.matches(account.password(), user.getPassword()))
                .ifPresent(user -> log.warn("SECURITY: account '{}' still uses its well-known default password. "
                        + "Change or disable it before production use.", username)));
    }

    private record DemoAccount(String password, String fullName, Role role, String department) {}
}
