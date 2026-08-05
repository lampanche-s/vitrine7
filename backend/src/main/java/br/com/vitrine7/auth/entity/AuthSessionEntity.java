package br.com.vitrine7.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthSessionEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_activity_at", nullable = false)
    private Instant lastActivityAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public static AuthSessionEntity create(
            UUID id,
            Long userId,
            Instant now,
            Instant expiresAt
    ) {
        AuthSessionEntity session = new AuthSessionEntity();

        session.id = id;
        session.userId = userId;
        session.createdAt = now;
        session.lastActivityAt = now;
        session.expiresAt = expiresAt;

        return session;
    }
}
