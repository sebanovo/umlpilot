package com.umlpilot.umlpilot_api.controller;

import com.umlpilot.umlpilot_api.dto.AuthResponse;
import com.umlpilot.umlpilot_api.dto.LoginRequest;
import com.umlpilot.umlpilot_api.dto.RefreshTokenRequest;
import com.umlpilot.umlpilot_api.dto.RegisterRequest;
import com.umlpilot.umlpilot_api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private final AuthService authService;

    public AuthenticationController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            HttpStatus status = message.contains("ya está registrado")
                    ? HttpStatus.CONFLICT
                    : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status)
                    .body(Map.of("error", message));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            AuthResponse response = authService.refreshToken(request.refreshToken());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
