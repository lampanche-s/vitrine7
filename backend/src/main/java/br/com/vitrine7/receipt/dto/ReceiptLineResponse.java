package br.com.vitrine7.receipt.dto;

public record ReceiptLineResponse(
        String description,
        String category,
        int quantity,
        long unitPriceCents,
        long totalCents
) {
}
