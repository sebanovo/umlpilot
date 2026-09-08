package com.umlpilot.umlpilot_api.service;

import com.umlpilot.umlpilot_api.model.Role;
import com.umlpilot.umlpilot_api.model.User;
import com.umlpilot.umlpilot_api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class SuperadminBootstrapService {

    private static final Logger log = LoggerFactory.getLogger(SuperadminBootstrapService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.superadmin.email}")
    private String superadminEmail;

    @Value("${app.bootstrap.superadmin.password}")
    private String superadminPassword;

    @Value("${app.bootstrap.superadmin.first-name}")
    private String superadminFirstName;

    @Value("${app.bootstrap.superadmin.last-name}")
    private String superadminLastName;

    public SuperadminBootstrapService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void bootstrapSuperadmin() {
        if (userRepository.existsByEmail(superadminEmail)) {
            log.info("Superadmin ya existe: {}", superadminEmail);
            return;
        }

        User superadmin = new User(
                superadminEmail,
                passwordEncoder.encode(superadminPassword),
                superadminFirstName,
                superadminLastName,
                Role.SUPERADMIN
        );

        userRepository.save(superadmin);
        log.info("Superadmin creado exitosamente: {}", superadminEmail);
    }
}
