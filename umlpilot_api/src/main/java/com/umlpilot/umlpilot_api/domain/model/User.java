package com.umlpilot.umlpilot_api.domain.model;

import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import java.time.LocalDateTime;

public class User {
    private final UserId id;
    private final String email;
    private final String password;
    private final String firstName;
    private final String lastName;
    private final Role role;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private User(UserId id, String email, String password, String firstName, String lastName, Role role, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static User create(String email, String password, String firstName, String lastName, Role role) {
        if (email == null || email.isBlank()) throw new DomainException("Email cannot be empty");
        if (password == null || password.isBlank()) throw new DomainException("Password cannot be empty");
        if (firstName == null || firstName.isBlank()) throw new DomainException("First name cannot be empty");
        if (lastName == null || lastName.isBlank()) throw new DomainException("Last name cannot be empty");
        LocalDateTime now = LocalDateTime.now();
        return new User(UserId.generate(), email, password, firstName, lastName, role, now, now);
    }

    public static User reconstitute(UserId id, String email, String password, String firstName, String lastName, Role role, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new User(id, email, password, firstName, lastName, role, createdAt, updatedAt);
    }

    public UserId getId() { return id; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public Role getRole() { return role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
