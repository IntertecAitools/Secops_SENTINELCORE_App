package com.sentinelcore.backend.service;

import com.sentinelcore.backend.dto.*;
import com.sentinelcore.backend.entity.Role;
import com.sentinelcore.backend.entity.User;
import com.sentinelcore.backend.exception.ConflictException;
import com.sentinelcore.backend.exception.TooManyRequestsException;
import com.sentinelcore.backend.repository.UserRepository;
import com.sentinelcore.backend.security.jwt.JwtService;
import com.sentinelcore.backend.security.service.LoginAttemptService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final LoginAttemptService loginAttemptService;
    private final AuditLogService auditLogService;
    private final boolean selfRegistrationEnabled;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           AuthenticationManager authenticationManager,
                           LoginAttemptService loginAttemptService,
                           AuditLogService auditLogService,
                           @Value("${app.auth.self-registration-enabled:false}") boolean selfRegistrationEnabled) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.loginAttemptService = loginAttemptService;
        this.auditLogService = auditLogService;
        this.selfRegistrationEnabled = selfRegistrationEnabled;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (!selfRegistrationEnabled) {
            auditLogService.logSecurityEvent("REGISTER_REJECTED", request.getUsername(),
                    "Self-registration is disabled");
            throw new AccessDeniedException("Self-registration is disabled");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already exists");
        }

        // The role is never taken from the request: self-registered accounts are always least-privileged.
        User user = User.builder()
                .fullName(request.getFullName())
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);
        auditLogService.logSecurityEvent("USER_SELF_REGISTERED", user.getUsername(), "Self-registered with role USER");

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String ip = AuditLogService.currentIpAddress();
        String attemptedUsername = request.getUsername();

        if (loginAttemptService.isBlocked(attemptedUsername, ip)) {
            auditLogService.logSecurityEvent("LOGIN_BLOCKED", attemptedUsername, "Too many failed login attempts");
            throw new TooManyRequestsException("Too many failed login attempts. Try again later.");
        }

        User user = userRepository.findByUsernameOrEmail(attemptedUsername, attemptedUsername).orElse(null);
        if (user == null) {
            // Same response as a wrong password so usernames cannot be enumerated
            loginAttemptService.recordFailure(attemptedUsername, ip);
            auditLogService.logSecurityEvent("LOGIN_FAILED", attemptedUsername, "Unknown account");
            throw new BadCredentialsException("Invalid username or password");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            user.getUsername(),
                            request.getPassword()
                    )
            );
        } catch (AuthenticationException e) {
            loginAttemptService.recordFailure(attemptedUsername, ip);
            auditLogService.logSecurityEvent("LOGIN_FAILED", user.getUsername(), e.getClass().getSimpleName());
            throw new BadCredentialsException("Invalid username or password");
        }

        loginAttemptService.recordSuccess(attemptedUsername, ip);

        // Update Last Login timestamp
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        auditLogService.logSecurityEvent("LOGIN_SUCCESS", user.getUsername(), "Authenticated");

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }
}
