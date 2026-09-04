package br.com.vitrine7.auth.repository;

import br.com.vitrine7.auth.entity.AuthSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface AuthSessionRepository extends
        JpaRepository<AuthSessionEntity, UUID> {

    @Modifying
    @Query("""
            UPDATE AuthSessionEntity session
               SET session.lastActivityAt = :now
                 , session.expiresAt = :expiresAt
             WHERE session.id = :sessionId
               AND session.revokedAt IS NULL
               AND session.lastActivityAt <= :threshold
            """)
    int touchIfOlderThan(
            @Param("sessionId") UUID sessionId,
            @Param("now") Instant now,
            @Param("expiresAt") Instant expiresAt,
            @Param("threshold") Instant threshold
    );

    @Modifying
    @Query("""
            UPDATE AuthSessionEntity session
               SET session.revokedAt = :now
             WHERE session.id = :sessionId
               AND session.revokedAt IS NULL
            """)
    int revoke(
            @Param("sessionId") UUID sessionId,
            @Param("now") Instant now
    );

    @Modifying
    @Query("""
            UPDATE AuthSessionEntity session
               SET session.revokedAt = :now
             WHERE session.userId = :userId
               AND session.revokedAt IS NULL
            """)
    int revokeAllByUserId(
            @Param("userId") Long userId,
            @Param("now") Instant now
    );
}
