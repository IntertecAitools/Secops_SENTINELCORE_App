package com.sentinelcore.backend.controller;

import com.sentinelcore.backend.dto.AuthResponse;
import com.sentinelcore.backend.dto.LoginRequest;
import com.sentinelcore.backend.dto.RegisterRequest;
import com.sentinelcore.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public AuthResponse register(
            @RequestBody RegisterRequest request) {

        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(
            @RequestBody LoginRequest request) {

        return authService.login(request);
    }
}