package br.com.vitrine7.checkout.dto;

public record CheckoutFinalizationResponse(
        CheckoutSessionResponse checkout,
        boolean finalizedNow,
        int processedItems
) {
}
