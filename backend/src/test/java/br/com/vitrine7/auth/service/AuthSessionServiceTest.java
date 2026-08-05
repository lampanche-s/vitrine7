package br.com.vitrine7.auth.service;

import br.com.vitrine7.auth.entity.AuthSessionEntity;
import br.com.vitrine7.auth.repository.AuthSessionRepository;
import br.com.vitrine7.common.security.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthSessionServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-07-22T12:00:00Z");

    private final AuthSessionRepository repository =
            mock(AuthSessionRepository.class);

    private final AuthSessionService service =
            new AuthSessionService(
                    repository,
                    new SecurityProperties(
                            "issuer",
                            "secret",
                            Duration.ofHours(8),
                            Duration.ofHours(5),
                            Duration.ofMinutes(5),
                            "AUTH",
                            false,
                            "Lax",
                            "http://localhost:5173",
                            new SecurityProperties.InitialAdmin(
                                    "Admin",
                                    "admin",
                                    "Password1!"
                            ),
                            null
                    ),
                    Clock.fixed(NOW, ZoneOffset.UTC)
            );

    @Test
    void acceptsSessionBeforeFiveHoursOfInactivity() {
        UUID sessionId = UUID.randomUUID();
        AuthSessionEntity session = AuthSessionEntity.create(
                sessionId,
                7L,
                NOW.minus(Duration.ofHours(4)),
                NOW.plus(Duration.ofHours(4))
        );

        when(repository.findById(sessionId))
                .thenReturn(Optional.of(session));

        assertTrue(service.validateAndTouch(
                jwt(sessionId, 7L),
                request(true)
        ));

        verify(repository).touchIfOlderThan(
                sessionId,
                NOW,
                NOW.plus(Duration.ofHours(5)),
                NOW.minus(Duration.ofMinutes(5))
        );
    }

    @Test
    void rejectsAndRevokesSessionAfterMoreThanFiveHoursOfInactivity() {
        UUID sessionId = UUID.randomUUID();
        AuthSessionEntity session = AuthSessionEntity.create(
                sessionId,
                7L,
                NOW.minus(Duration.ofHours(6)),
                NOW.plus(Duration.ofHours(2))
        );

        when(repository.findById(sessionId))
                .thenReturn(Optional.of(session));

        assertFalse(service.validateAndTouch(
                jwt(sessionId, 7L),
                request(true)
        ));

        verify(repository).revoke(sessionId, NOW);
        verify(repository, never()).touchIfOlderThan(
                sessionId,
                NOW,
                NOW.plus(Duration.ofHours(5)),
                NOW.minus(Duration.ofMinutes(5))
        );
    }

    @Test
    void rejectsSessionWhoseDatabaseExpirationWasReached() {
        UUID sessionId = UUID.randomUUID();
        AuthSessionEntity session = AuthSessionEntity.create(
                sessionId,
                7L,
                NOW.minus(Duration.ofMinutes(30)),
                NOW
        );

        when(repository.findById(sessionId))
                .thenReturn(Optional.of(session));

        assertFalse(service.validateAndTouch(
                jwt(sessionId, 7L),
                request(true)
        ));

        verify(repository, never()).touchIfOlderThan(
                sessionId,
                NOW,
                NOW.plus(Duration.ofHours(5)),
                NOW.minus(Duration.ofMinutes(5))
        );
    }

    @Test
    void silentRequestDoesNotTouchSession() {
        UUID sessionId = UUID.randomUUID();
        AuthSessionEntity session = AuthSessionEntity.create(
                sessionId,
                7L,
                NOW.minus(Duration.ofMinutes(30)),
                NOW.plus(Duration.ofHours(4))
        );

        when(repository.findById(sessionId))
                .thenReturn(Optional.of(session));

        assertTrue(service.validateAndTouch(
                jwt(sessionId, 7L),
                request(false)
        ));

        verify(repository, never()).touchIfOlderThan(
                sessionId,
                NOW,
                NOW.plus(Duration.ofHours(5)),
                NOW.minus(Duration.ofMinutes(5))
        );
    }

    @Test
    void logoutRevokesSessionExposedByAuthenticationFilter() {
        UUID sessionId = UUID.randomUUID();
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getAttribute(
                AuthSessionService.CURRENT_SESSION_ID_ATTRIBUTE
        )).thenReturn(sessionId);

        service.revokeCurrentSession(request);

        verify(repository).revoke(sessionId, NOW);
    }

    @Test
    void securityChangeRevokesEveryOpenSessionForUser() {
        service.revokeAllForUser(7L);

        verify(repository).revokeAllByUserId(7L, NOW);
    }

    private Jwt jwt(UUID sessionId, long userId) {
        return new Jwt(
                "token",
                NOW.minusSeconds(60),
                NOW.plus(Duration.ofHours(8)),
                Map.of("alg", "none"),
                Map.of(
                        "iss", "issuer",
                        "sub", "admin",
                        "jti", sessionId.toString(),
                        "userId", userId
                )
        );
    }

    private HttpServletRequest request(
            boolean userActivity
    ) {
        HttpServletRequest request =
                mock(HttpServletRequest.class);

        when(request.getHeader("X-User-Activity"))
                .thenReturn(userActivity ? "true" : null);

        return request;
    }
}
