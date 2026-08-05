package br.com.vitrine7.lava.workorder.service;

import br.com.vitrine7.checkout.entity.CheckoutDocumentType;
import br.com.vitrine7.common.exception.InvalidRequestException;
import br.com.vitrine7.lava.client.entity.LavaClientEntity;
import br.com.vitrine7.lava.workorder.entity.LavaVehicleSize;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class LavaWorkOrderNormalizer {

    private static final Pattern OLD_PLATE_PATTERN =
            Pattern.compile("^[A-Z]{3}[0-9]{4}$");

    private static final Pattern MERCOSUL_PLATE_PATTERN =
            Pattern.compile("^[A-Z]{3}[0-9][A-Z][0-9]{2}$");

    public NormalizedCustomer normalizeCustomer(
            Long clientId,
            String rawCustomerName,
            String rawPhone,
            String rawVehicleName,
            String rawPlate,
            LavaClientEntity client
    ) {
        if (clientId != null) {
            if (client == null) {
                throw new IllegalArgumentException(
                        "client must be loaded when clientId is present"
                );
            }

            return new NormalizedCustomer(
                    client.getId(),
                    client.getName(),
                    client.getNormalizedName(),
                    client.getPhoneDigits(),
                    client.getVehicleName(),
                    client.getNormalizedVehicleName(),
                    client.getPlate()
            );
        }

        String customerName = normalizeOptionalText(rawCustomerName);
        if (customerName == null) {
            customerName = "Cliente avulso";
        }

        if (customerName.length() > 120) {
            throw new InvalidRequestException(
                    "LAVA_WORK_ORDER_CUSTOMER_NAME_TOO_LONG",
                    "O nome do cliente deve possuir no maximo 120 caracteres."
            );
        }

        String phoneDigits = normalizePhone(rawPhone);
        String vehicleName = normalizeOptionalText(rawVehicleName);

        if (vehicleName != null && vehicleName.length() > 120) {
            throw new InvalidRequestException(
                    "LAVA_WORK_ORDER_VEHICLE_NAME_TOO_LONG",
                    "O veiculo deve possuir no maximo 120 caracteres."
            );
        }

        String plate = normalizePlate(rawPlate, false);

        return new NormalizedCustomer(
                null,
                customerName,
                normalizeForSearch(customerName),
                phoneDigits,
                vehicleName,
                vehicleName == null
                        ? null
                        : normalizeForSearch(vehicleName),
                plate
        );
    }

    public NormalizedPreparation normalizePreparation(
            CheckoutDocumentType documentType,
            String rawCpf,
            Long discountCents
    ) {
        long discount = discountCents == null ? 0L : discountCents;

        if (discount < 0) {
            throw new InvalidRequestException(
                    "INVALID_CHECKOUT_DISCOUNT",
                    "O desconto e invalido."
            );
        }

        if (documentType != CheckoutDocumentType.GENERAL_RECEIPT) {
            throw new InvalidRequestException(
                    "INVALID_CHECKOUT_DOCUMENT",
                    "A ordem de servico deve utilizar recibo geral."
            );
        }

        if (rawCpf != null && !rawCpf.isBlank()) {
            throw new InvalidRequestException(
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

    public String normalizeCancelReason(String reason) {
        String normalized = normalizeOptionalText(reason);

        if (normalized == null
                || normalized.length() < 3
                || normalized.length() > 255) {
            throw new InvalidRequestException(
                    "INVALID_LAVA_WORK_ORDER_CANCEL_REASON",
                    "O motivo do cancelamento deve possuir entre 3 e 255 caracteres."
            );
        }

        return normalized;
    }

    public String normalizeSearchText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return normalizeForSearch(value);
    }

    public String digitsOnlyOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String digits = value.replaceAll("\\D", "");
        return digits.isBlank() ? null : digits;
    }

    public String normalizePlateSearch(String value) {
        String plate = normalizePlate(value, true);
        return plate;
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value
                .trim()
                .replaceAll("\\s+", " ");

        return normalized.isBlank() ? null : normalized;
    }

    private String normalizePhone(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String digits = value.replaceAll("\\D", "");

        if (digits.length() != 10 && digits.length() != 11) {
            throw new InvalidRequestException(
                    "INVALID_CLIENT_PHONE",
                    "O telefone deve possuir 10 ou 11 digitos."
            );
        }

        return digits;
    }

    private String normalizePlate(String value, boolean allowPartial) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String plate = value
                .replaceAll("[^A-Za-z0-9]", "")
                .toUpperCase(Locale.ROOT);

        if (allowPartial) {
            return plate.isBlank() ? null : plate;
        }

        boolean valid =
                OLD_PLATE_PATTERN.matcher(plate).matches()
                        || MERCOSUL_PLATE_PATTERN
                        .matcher(plate)
                        .matches();

        if (!valid) {
            throw new InvalidRequestException(
                    "INVALID_VEHICLE_PLATE",
                    "A placa deve seguir o formato AAA1234 ou AAA1A23."
            );
        }

        return plate;
    }

    private String normalizeForSearch(String value) {
        String withoutAccents = Normalizer
                .normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return withoutAccents
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    public record NormalizedCustomer(
            Long clientId,
            String customerName,
            String normalizedCustomerName,
            String phoneDigits,
            String vehicleName,
            String normalizedVehicleName,
            String plate
    ) {
    }

    public record NormalizedPreparation(
            CheckoutDocumentType documentType,
            String cpfDigits,
            long discountCents
    ) {
    }
}
