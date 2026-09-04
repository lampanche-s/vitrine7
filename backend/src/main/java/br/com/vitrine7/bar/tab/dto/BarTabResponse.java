package br.com.vitrine7.bar.tab.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record BarTabResponse(
        Long id,
        String name,
        Long clientId,
        String clientName,
        Long employeeId,
        String closureType,
        String status,
        UUID checkoutId,
        String checkoutStatus,
        Long subtotalCents,
        Long discountCents,
        Long totalCents,
        String documentType,
        String cpfDigits,
        boolean prepared,
        OffsetDateTime preparedAt,
        OffsetDateTime closedAt,
        String vehicleName,
        String vehiclePlate,
        OffsetDateTime reopenUntil,
        boolean canReopen,
        List<BarTabLineResponse> lines,
        Long createdByUserId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public BarTabResponse(
            Long id, String name, Long clientId, String clientName, String status,
            UUID checkoutId, String checkoutStatus, Long subtotalCents, Long discountCents,
            Long totalCents, String documentType, String cpfDigits, boolean prepared,
            OffsetDateTime preparedAt, OffsetDateTime closedAt, String vehicleName,
            String vehiclePlate, OffsetDateTime reopenUntil, boolean canReopen,
            List<BarTabLineResponse> lines, Long createdByUserId, OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this(id, name, clientId, clientName, null, null, status, checkoutId, checkoutStatus,
                subtotalCents, discountCents, totalCents, documentType, cpfDigits, prepared,
                preparedAt, closedAt, vehicleName, vehiclePlate, reopenUntil, canReopen,
                lines, createdByUserId, createdAt, updatedAt);
    }

    public BarTabResponse(
            Long id, String name, Long clientId, String clientName, String status,
            UUID checkoutId, String checkoutStatus, Long subtotalCents, Long discountCents,
            Long totalCents, String documentType, String cpfDigits, boolean prepared,
            OffsetDateTime preparedAt, OffsetDateTime closedAt, OffsetDateTime reopenUntil,
            boolean canReopen, List<BarTabLineResponse> lines, Long createdByUserId,
            OffsetDateTime createdAt, OffsetDateTime updatedAt
    ) {
        this(id, name, clientId, clientName, null, null, status, checkoutId, checkoutStatus, subtotalCents,
                discountCents, totalCents, documentType, cpfDigits, prepared, preparedAt, closedAt,
                null, null, reopenUntil, canReopen, lines, createdByUserId, createdAt, updatedAt);
    }
}
