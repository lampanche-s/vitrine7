package br.com.vitrine7.auth.service;

import br.com.vitrine7.common.security.SecurityProperties;
import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final SecurityProperties securityProperties;

    public GeneratedToken generate(VitrineUserPrincipal principal) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(
                securityProperties.jwtExpiration()
        );

        UUID sessionId = UUID.randomUUID();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(securityProperties.jwtIssuer())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(principal.getUsername())
                .id(sessionId.toString())
                .claim("userId", principal.getId())
                .claim("name", principal.getName())
                .claim("role", principal.getRole().name())
                .claim("permissions", principal.getPermissionCodes())
                .claim("authVersion", principal.getAuthVersion())
                .build();

        String token = jwtEncoder.encode(
                JwtEncoderParameters.from(claims)
        ).getTokenValue();

        return new GeneratedToken(token, expiresAt, sessionId);
    }

    public Jwt decode(String token) {
        return jwtDecoder.decode(token);
    }

    public record GeneratedToken(
            String value,
            Instant expiresAt,
            UUID sessionId
    ) {
    }
}
