package br.com.vitrine7.receipt.dto;

import java.util.UUID;

public record ReceiptOperationResponse(
        String type,
        Long operationId,
        UUID checkoutId,
        String displayName,
        String status,
        Long responsibleUserId,
        String responsibleUserName,
        String vehicleName,
        String vehiclePlate
) {
    public ReceiptOperationResponse(
            String type, Long operationId, UUID checkoutId, String displayName,
            String status, Long responsibleUserId, String responsibleUserName
    ) {
        this(type, operationId, checkoutId, displayName, status, responsibleUserId,
                responsibleUserName, null, null);
    }
}
