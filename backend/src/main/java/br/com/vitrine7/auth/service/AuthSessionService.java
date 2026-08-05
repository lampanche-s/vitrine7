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

    public static final String CURRENT_SESSION_ID_ATTRIBUTE =
            AuthSessionService.class.getName() + ".currentSessionId";

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
                        earlierOf(
                                inactivityExpiresAt,
                                tokenExpiresAt
                        )
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
        Instant tokenExpiresAt = jwt.getExpiresAt();

        if (tokenExpiresAt == null
                || !tokenExpiresAt.isAfter(now)) {
            return false;
        }

        return authSessionRepository.findById(sessionId)
                .filter(session -> session.getRevokedAt() == null)
                .filter(session -> matchesUser(jwt, session))
                .filter(session -> session.getExpiresAt().isAfter(now))
                .filter(session -> {
                    boolean active = session
                            .getLastActivityAt()
                            .plus(securityProperties.inactivityExpiration())
                            .isAfter(now);

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
                                earlierOf(
                                        now.plus(
                                                securityProperties
                                                        .inactivityExpiration()
                                        ),
                                        tokenExpiresAt
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

    public void exposeCurrentSession(
            Jwt jwt,
            HttpServletRequest request
    ) {
        request.setAttribute(
                CURRENT_SESSION_ID_ATTRIBUTE,
                extractSessionId(jwt)
        );
    }

    @Transactional
    public void revokeCurrentSession(
            HttpServletRequest request
    ) {
        Object sessionId = request.getAttribute(
                CURRENT_SESSION_ID_ATTRIBUTE
        );

        if (sessionId instanceof UUID id) {
            authSessionRepository.revoke(
                    id,
                    clock.instant()
            );
        }
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        authSessionRepository.revokeAllByUserId(
                userId,
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

    private Instant earlierOf(
            Instant first,
            Instant second
    ) {
        return first.isBefore(second)
                ? first
                : second;
    }
}
