package com.sentinelcore.backend.support;

import com.sentinelcore.backend.entity.Role;
import com.sentinelcore.backend.entity.User;
import com.sentinelcore.backend.repository.UserRepository;
import com.sentinelcore.backend.security.jwt.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base class for MockMvc integration tests. Creates uniquely named test users (explicitly labelled
 * fixtures, never business data) and issues real JWTs so the full security filter chain is exercised.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTestSupport {

    public static final String TEST_PASSWORD = "Test-Fixture-Passw0rd";
    private static final AtomicInteger COUNTER = new AtomicInteger();

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected JwtService jwtService;

    protected User createUser(Role role, String tenantKey) {
        int n = COUNTER.incrementAndGet();
        String username = "fixture-" + role.name().toLowerCase() + "-" + n;
        return userRepository.save(User.builder()
                .username(username)
                .email(username + "@test.invalid")
                .fullName("Test Fixture " + n)
                .password(passwordEncoder.encode(TEST_PASSWORD))
                .role(role)
                .enabled(true)
                .tenantKey(tenantKey)
                .createdAt(LocalDateTime.now())
                .build());
    }

    protected User createUser(Role role) {
        return createUser(role, null);
    }

    protected String bearer(User user) {
        return "Bearer " + jwtService.generateToken(user);
    }

    protected String bearer(Role role) {
        return bearer(createUser(role));
    }
}
