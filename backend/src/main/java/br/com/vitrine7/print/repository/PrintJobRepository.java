package br.com.vitrine7.print.repository;

import br.com.vitrine7.print.dto.PrintJobDtos;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PrintJobRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PrintJobRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void create(
            UUID id,
            UUID checkoutId,
            Long requestedByUserId,
            String documentKind,
            String receiptText
    ) {
        jdbcTemplate.update(
                """
                        INSERT INTO print_jobs (
                            id,
                            checkout_session_id,
                            requested_by_user_id,
                            document_kind,
                            status,
                            receipt_text
                        ) VALUES (
                            :id,
                            :checkoutId,
                            :requestedByUserId,
                            :documentKind,
                            'PENDING',
                            :receiptText
                        )
                        """,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("checkoutId", checkoutId)
                        .addValue("requestedByUserId", requestedByUserId)
                        .addValue("documentKind", documentKind)
                        .addValue("receiptText", receiptText)
        );
    }

    public void createCashClosing(
            UUID id,
            Long requestedByUserId,
            LocalDate businessDate,
            String receiptText
    ) {
        jdbcTemplate.update(
                """
                        INSERT INTO print_jobs (
                            id,
                            checkout_session_id,
                            requested_by_user_id,
                            business_date,
                            document_kind,
                            status,
                            receipt_text
                        ) VALUES (
                            :id,
                            NULL,
                            :requestedByUserId,
                            :businessDate,
                            'CASH_CLOSING',
                            'PENDING',
                            :receiptText
                        )
                        """,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("requestedByUserId", requestedByUserId)
                        .addValue("businessDate", businessDate)
                        .addValue("receiptText", receiptText)
        );
    }

    public Optional<PrintJobDtos.Created> findActiveCashClosing(
            long requestedByUserId,
            LocalDate businessDate
    ) {
        return jdbcTemplate.query(
                """
                        SELECT id, status
                        FROM print_jobs
                        WHERE requested_by_user_id = :requestedByUserId
                          AND business_date = :businessDate
                          AND document_kind = 'CASH_CLOSING'
                          AND status IN ('PENDING', 'PRINTING')
                        ORDER BY created_at, id
                        LIMIT 1
                        """,
                new MapSqlParameterSource()
                        .addValue("requestedByUserId", requestedByUserId)
                        .addValue("businessDate", businessDate),
                (resultSet, rowNumber) -> new PrintJobDtos.Created(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("status")
                )
        ).stream().findFirst();
    }

    public Optional<PrintJobDtos.Created> findActiveByCheckoutId(
            UUID checkoutId,
            String documentKind
    ) {
        return jdbcTemplate.query(
                """
                        SELECT id, status
                        FROM print_jobs
                        WHERE checkout_session_id = :checkoutId
                          AND document_kind = :documentKind
                          AND status IN ('PENDING', 'PRINTING')
                        ORDER BY created_at, id
                        LIMIT 1
                        """,
                new MapSqlParameterSource()
                        .addValue("checkoutId", checkoutId)
                        .addValue("documentKind", documentKind),
                (resultSet, rowNumber) -> new PrintJobDtos.Created(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("status")
                )
        ).stream().findFirst();
    }

    public Optional<PrintJobDtos.Status> findStatus(UUID id) {
        return jdbcTemplate.query(
                """
                        SELECT
                            id,
                            status,
                            error_message,
                            completed_at
                        FROM print_jobs
                        WHERE id = :id
                        """,
                new MapSqlParameterSource("id", id),
                (resultSet, rowNumber) -> new PrintJobDtos.Status(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("status"),
                        resultSet.getString("error_message"),
                        resultSet.getObject("completed_at", OffsetDateTime.class)
                )
        ).stream().findFirst();
    }

    public Optional<PrintJobDtos.Delivery> reserveNext(
            OffsetDateTime now
    ) {
        return jdbcTemplate.query(
                """
                        WITH candidate AS (
                            SELECT id
                            FROM print_jobs
                            WHERE status = 'PENDING'
                            ORDER BY created_at, id
                            FOR UPDATE SKIP LOCKED
                            LIMIT 1
                        )
                        UPDATE print_jobs job
                        SET
                            status = 'PRINTING',
                            printing_started_at = :now,
                            attempts = attempts + 1,
                            error_message = NULL,
                            updated_at = :now
                        FROM candidate
                        WHERE job.id = candidate.id
                        RETURNING
                            job.id,
                            job.receipt_text,
                            job.attempts
                        """,
                new MapSqlParameterSource()
                        .addValue("now", now),
                (resultSet, rowNumber) -> new PrintJobDtos.Delivery(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("receipt_text"),
                        resultSet.getInt("attempts")
                )
        ).stream().findFirst();
    }

    public boolean complete(
            UUID id,
            boolean success,
            String errorMessage,
            OffsetDateTime now
    ) {
        int updated = jdbcTemplate.update(
                """
                        UPDATE print_jobs
                        SET
                            status = :status,
                            completed_at = :now,
                            error_message = :errorMessage,
                            updated_at = :now
                        WHERE id = :id
                          AND status = 'PRINTING'
                        """,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("status", success ? "PRINTED" : "FAILED")
                        .addValue("errorMessage", success ? null : normalizeError(errorMessage))
                        .addValue("now", now)
        );

        return updated == 1;
    }

    private String normalizeError(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return "Falha de impressao informada pelo agente.";
        }

        String normalized = errorMessage.trim();
        return normalized.length() <= 500
                ? normalized
                : normalized.substring(0, 500);
    }
}
