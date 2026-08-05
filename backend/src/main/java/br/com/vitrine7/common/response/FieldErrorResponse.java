package br.com.vitrine7.common.response;

public record FieldErrorResponse(
        String field,
        String message
) {
}
