package br.com.vitrine7.auth.dto;

import java.time.Instant;

public record LoginResponse(
        CurrentUserResponse user,
        Instant expiresAt
) {
}
