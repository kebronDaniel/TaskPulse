package com.prep.taskpulse.domain.user;

import com.prep.taskpulse.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Getter
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String fullName;
    @Column(nullable = false, unique = true, length = 150)
    private String email;
    @Column(nullable = false)
    private String passwordHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER;

    private User(String fullName, String email, String passwordHash, Role role) {
        if (fullName == null || fullName.isBlank()) throw new IllegalArgumentException("full name must not be null");
        if (email == null || email.isBlank()) throw new IllegalArgumentException("email must not be null");
        if (passwordHash == null || passwordHash.isBlank()) throw new IllegalArgumentException("password must not be null");
        if (role == null) throw new IllegalArgumentException("user role must not be null");

        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public static User createUser(String fullName, String email, String passwordHash, Role role) {
        return new User(fullName,email,passwordHash,role);
    }

    public void changeFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("full name must not be blank");
        }
        this.fullName = fullName;
    }

    public void changeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        this.email = email;
    }

    public void changeRole(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("role must not be null");
        }
        this.role = role;
    }

    public void changePassword(String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("password hash must not be blank");
        }
        this.passwordHash = passwordHash;
    }
}
