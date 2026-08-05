package br.com.vitrine7.auth.dto;

public record CsrfResponse(
        String headerName,
        String parameterName,
        String token
) {
}
