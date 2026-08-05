package br.com.vitrine7.auth.service;

import br.com.vitrine7.auth.entity.AuthSessionEntity;
import br.com.vitrine7.auth.repository.AuthSessionRepository;
import br.com.vitrine7.common.security.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthSessionService {

    private static final String USER_ACTIVITY_HEADER =
            "X-User-Activity";

    private final AuthSessionRepository authSessionRepository;
    private final SecurityProperties securityProperties;
    private final Clock clock;

    @Transactional
    public void createSession(
            UUID sessionId,
            Long userId,
            Instant tokenExpiresAt
    ) {
        Instant now = clock.instant();
        Instant inactivityExpiresAt = now.plus(
                securityProperties.inactivityExpiration()
        );

        authSessionRepository.save(
                AuthSessionEntity.create(
                        sessionId,
                        userId,
                        now,
                        inactivityExpiresAt.isBefore(tokenExpiresAt)
                                ? inactivityExpiresAt
                                : tokenExpiresAt
                )
        );
    }

    @Transactional
    public boolean validateAndTouch(
            Jwt jwt,
            HttpServletRequest request
    ) {
        UUID sessionId = extractSessionId(jwt);
        Instant now = clock.instant();

        return authSessionRepository.findById(sessionId)
                .filter(session -> session.getRevokedAt() == null)
                .filter(session -> matchesUser(jwt, session))
                .filter(session -> {
                    boolean active = !session
                            .getLastActivityAt()
                            .plus(securityProperties.inactivityExpiration())
                            .isBefore(now);

                    if (!active) {
                        authSessionRepository.revoke(sessionId, now);
                    }

                    return active;
                })
                .map(session -> {
                    if (isUserActivity(request)) {
                        authSessionRepository.touchIfOlderThan(
                                sessionId,
                                now,
                                now.plus(
                                        securityProperties
                                                .inactivityExpiration()
                                ),
                                now.minus(
                                        securityProperties
                                                .activityTouchInterval()
                                )
                        );
                    }

                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public void revoke(Jwt jwt) {
        authSessionRepository.revoke(
                extractSessionId(jwt),
                clock.instant()
        );
    }

    private UUID extractSessionId(Jwt jwt) {
        return UUID.fromString(jwt.getId());
    }

    private boolean matchesUser(
            Jwt jwt,
            AuthSessionEntity session
    ) {
        Object claim = jwt.getClaim("userId");

        return claim instanceof Number number
                && number.longValue() == session.getUserId();
    }

    private boolean isUserActivity(
            HttpServletRequest request
    ) {
        return "true".equalsIgnoreCase(
                request.getHeader(USER_ACTIVITY_HEADER)
        );
    }
}
