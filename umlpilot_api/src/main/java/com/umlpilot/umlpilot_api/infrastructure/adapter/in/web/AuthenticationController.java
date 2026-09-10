package com.umlpilot.umlpilot_api.infrastructure.adapter.in.web;

import com.umlpilot.umlpilot_api.application.dto.*;
import com.umlpilot.umlpilot_api.application.service.AuthService;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.infrastructure.adapter.in.web.request.*;
import com.umlpilot.umlpilot_api.infrastructure.adapter.in.web.response.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            AuthResult result = authService.login(new LoginCommand(request.email(), request.password()));
            return ResponseEntity.ok(toResponse(result));
        } catch (DomainException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResult result = authService.register(new RegisterCommand(request.email(), request.password(), request.firstName(), request.lastName()));
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(result));
        } catch (DomainException e) {
            String message = e.getMessage();
            HttpStatus status = message.contains("ya está registrado") ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(Map.of("error", message));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            AuthResult result = authService.refreshToken(request.refreshToken());
            return ResponseEntity.ok(toResponse(result));
        } catch (DomainException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    private AuthResponse toResponse(AuthResult result) {
        return new AuthResponse(
                result.accessToken(), result.refreshToken(), result.tokenType(),
                new AuthResponse.UserDto(result.user().id(), result.user().email(), result.user().firstName(), result.user().lastName(), result.user().role())
        );
    }
}
