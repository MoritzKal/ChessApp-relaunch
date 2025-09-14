package com.chessapp.api.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    // store as csv for simplicity
    @Column(name = "roles_csv", nullable = false, length = 255)
    private String rolesCsv = "USER"; // default

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRolesCsv() { return rolesCsv; }
    public void setRolesCsv(String rolesCsv) { this.rolesCsv = rolesCsv; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (rolesCsv == null || rolesCsv.isBlank()) {
            rolesCsv = "USER";
        }
    }

    @Transient
    public List<String> getRoles() {
        String csv = rolesCsv == null ? "" : rolesCsv;
        String[] parts = csv.split(",");
        List<String> roles = new ArrayList<>();
        for (String p : parts) {
            String r = p.trim();
            if (!r.isEmpty()) roles.add(r);
        }
        return roles;
    }

    public void setRoles(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            this.rolesCsv = "USER";
        } else {
            this.rolesCsv = String.join(",", roles);
        }
    }
}
