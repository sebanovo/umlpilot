package com.umlpilot.umlpilot_api.service;

import com.umlpilot.umlpilot_api.dto.AuthResponse;
import com.umlpilot.umlpilot_api.dto.LoginRequest;
import com.umlpilot.umlpilot_api.dto.RegisterRequest;
import com.umlpilot.umlpilot_api.model.Role;
import com.umlpilot.umlpilot_api.model.User;
import com.umlpilot.umlpilot_api.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("Credenciales inválidas");
        }

        return buildAuthResponse(user);
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }

        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.firstName(),
                request.lastName(),
                Role.USER
        );

        User savedUser = userRepository.save(user);
        return buildAuthResponse(savedUser);
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtService.isTokenValid(refreshToken)) {
            throw new IllegalArgumentException("Refresh token inválido o expirado");
        }

        String email = jwtService.extractEmail(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        AuthResponse.UserDto userDto = new AuthResponse.UserDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name()
        );

        return new AuthResponse(accessToken, refreshToken, "Bearer", userDto);
    }
}
