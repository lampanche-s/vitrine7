package br.com.vitrine7.receipt.dto;

public record ReceiptEstablishmentResponse(
        String name,
        String document,
        String phone,
        String address
) {
}
