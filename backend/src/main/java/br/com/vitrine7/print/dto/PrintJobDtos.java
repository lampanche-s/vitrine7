package br.com.vitrine7.print.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class PrintJobDtos {

    private PrintJobDtos() {
    }

    public record Created(
            UUID id,
            String status
    ) {
    }

    public record Status(
            UUID id,
            String status,
            String errorMessage,
            OffsetDateTime completedAt
    ) {
    }

    public record Delivery(
            UUID id,
            String receiptText,
            int attempt
    ) {
    }

    public record ResultRequest(
            boolean success,
            String errorMessage
    ) {
    }

    public record ResultResponse(
            UUID id,
            String status
    ) {
    }
}
