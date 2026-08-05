package br.com.vitrine7.system.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.Locale;
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 255)
    private String username;

    @Column(length = 160)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserStatus status;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(name = "auth_version", nullable = false)
    private Long authVersion = 0L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "deleted_by_user_id")
    private Long deletedByUserId;

    public static UserEntity create(
            String name,
            String username,
            String email,
            String passwordHash,
            UserRole role,
            UserStatus status
    ) {
        UserEntity user = new UserEntity();

        user.name = name;
        user.username = username;
        user.email = email;
        user.passwordHash = passwordHash;
        user.role = role;
        user.status = status;
        user.authVersion = 0L;

        return user;
    }

    public void incrementAuthVersion() {
        this.authVersion = this.authVersion == null
                ? 1L
                : this.authVersion + 1L;
    }

    public void softDelete(Long actorUserId) {
        this.status = UserStatus.BLOQUEADO;
        this.deletedAt = OffsetDateTime.now();
        this.deletedByUserId = actorUserId;
        incrementAuthVersion();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    @PrePersist
    @PreUpdate
    private void normalize() {
        this.name = normalizeRequired(name);
        this.username = normalizeRequired(username);
        this.email = normalizeOptional(email);
    }

    private String normalizeRequired(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim().toLowerCase(Locale.ROOT);
    }
}
