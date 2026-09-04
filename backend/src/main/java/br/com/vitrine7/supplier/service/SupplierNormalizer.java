package br.com.vitrine7.supplier.service;

import br.com.vitrine7.common.exception.InvalidRequestException;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;

@Component
public class SupplierNormalizer {
    public NormalizedSupplierData normalize(String rawName, String rawCnpj, String rawPhone, String rawCep) {
        String name = normalizeRequiredName(rawName);
        return new NormalizedSupplierData(name, normalizedSearch(name), digits(rawCnpj, 14, "INVALID_SUPPLIER_CNPJ", "O CNPJ deve possuir 14 dígitos."), phone(rawPhone), digits(rawCep, 8, "INVALID_SUPPLIER_CEP", "O CEP deve possuir 8 dígitos."));
    }

    private String normalizeRequiredName(String value) {
        String name = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (name.isBlank()) {
            throw new InvalidRequestException("INVALID_SUPPLIER_NAME", "Informe um nome válido para o fornecedor.");
        }
        if (name.length() > 120) {
            throw new InvalidRequestException("SUPPLIER_NAME_TOO_LONG", "O nome deve possuir no máximo 120 caracteres.");
        }
        return name;
    }

    private String phone(String value) {
        if (value == null || value.isBlank()) return null;
        String digits = value.replaceAll("\\D", "");
        if (digits.length() != 10 && digits.length() != 11) {
            throw new InvalidRequestException("INVALID_SUPPLIER_PHONE", "O telefone deve possuir 10 ou 11 dígitos.");
        }
        return digits;
    }

    private String digits(String value, int length, String code, String message) {
        if (value == null || value.isBlank()) return null;
        String digits = value.replaceAll("\\D", "");
        if (digits.length() != length) throw new InvalidRequestException(code, message);
        return digits;
    }

    private String normalizedSearch(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    public record NormalizedSupplierData(String name, String normalizedName, String cnpjDigits, String phoneDigits, String cepDigits) { }
}
