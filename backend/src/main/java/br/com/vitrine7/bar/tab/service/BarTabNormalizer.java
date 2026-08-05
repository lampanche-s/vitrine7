package br.com.vitrine7.bar.tab.service;

import br.com.vitrine7.checkout.entity.CheckoutDocumentType;
import br.com.vitrine7.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;

@Component
public class BarTabNormalizer {

    public NormalizedName normalizeName(String value) {
        String name = value
                .trim()
                .replaceAll("\\s+", " ");

        if (name.isBlank() || name.length() > 80) {
            throw new BusinessException(
                    "INVALID_BAR_TAB_NAME",
                    "O nome da comanda deve possuir entre 1 e 80 caracteres."
            );
        }

        String normalized = Normalizer
                .normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);

        return new NormalizedName(name, normalized);
    }

    public NormalizedPreparation normalizePreparation(
            CheckoutDocumentType documentType,
            String cpf,
            Long discountCents
    ) {
        long discount = discountCents == null
                ? 0L
                : discountCents;

        if (documentType != CheckoutDocumentType.GENERAL_RECEIPT) {
            throw new BusinessException(
                    "INVALID_CHECKOUT_DOCUMENT",
                    "A comanda deve utilizar recibo geral."
            );
        }

        if (cpf != null && !cpf.isBlank()) {
            throw new BusinessException(
                    "CHECKOUT_CPF_NOT_ALLOWED",
                    "CPF nao e aceito neste fluxo."
            );
        }

        return new NormalizedPreparation(
                CheckoutDocumentType.GENERAL_RECEIPT,
                null,
                discount
        );
    }

    public record NormalizedName(
            String name,
            String normalizedName
    ) {
    }

    public record NormalizedPreparation(
            CheckoutDocumentType documentType,
            String cpfDigits,
            long discountCents
    ) {
    }
}
