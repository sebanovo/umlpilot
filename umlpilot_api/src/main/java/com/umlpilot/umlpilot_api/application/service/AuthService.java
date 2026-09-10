package com.umlpilot.umlpilot_api.application.service;

import com.umlpilot.umlpilot_api.application.dto.AuthResult;
import com.umlpilot.umlpilot_api.application.dto.LoginCommand;
import com.umlpilot.umlpilot_api.application.dto.RegisterCommand;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.domain.model.User;
import com.umlpilot.umlpilot_api.domain.model.UserId;
import com.umlpilot.umlpilot_api.domain.repository.UserRepository;
import com.umlpilot.umlpilot_api.infrastructure.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public AuthResult login(LoginCommand command) {
        User user = userRepository.findByEmail(command.email())
                .orElseThrow(() -> new DomainException("Credenciales inválidas"));

        if (!passwordEncoder.matches(command.password(), user.getPassword())) {
            throw new DomainException("Credenciales inválidas");
        }

        return buildAuthResult(user);
    }

    @Transactional
    public AuthResult register(RegisterCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new DomainException("El email ya está registrado");
        }

        User user = User.create(
                command.email(),
                passwordEncoder.encode(command.password()),
                command.firstName(),
                command.lastName(),
                com.umlpilot.umlpilot_api.domain.model.Role.USER
        );

        User savedUser = userRepository.save(user);
        return buildAuthResult(savedUser);
    }

    @Transactional
    public AuthResult refreshToken(String refreshToken) {
        if (!jwtService.isTokenValid(refreshToken)) {
            throw new DomainException("Refresh token inválido o expirado");
        }

        String email = jwtService.extractEmail(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new DomainException("Usuario no encontrado"));

        return buildAuthResult(user);
    }

    private AuthResult buildAuthResult(User user) {
        String accessToken = jwtService.generateAccessToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        AuthResult.UserResult userResult = new AuthResult.UserResult(
                user.getId().value(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name()
        );

        return new AuthResult(accessToken, refreshToken, "Bearer", userResult);
    }
}
