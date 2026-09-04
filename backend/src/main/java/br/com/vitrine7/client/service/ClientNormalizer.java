package br.com.vitrine7.client.service;

import br.com.vitrine7.common.exception.InvalidRequestException;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class ClientNormalizer {

    private static final Pattern OLD_PLATE_PATTERN =
            Pattern.compile("^[A-Z]{3}[0-9]{4}$");

    private static final Pattern MERCOSUL_PLATE_PATTERN =
            Pattern.compile("^[A-Z]{3}[0-9][A-Z][0-9]{2}$");

    public NormalizedClientData normalize(
            String rawName,
            String rawPhone,
            String rawVehicleName,
            String rawPlate
    ) {
        String name = normalizeRequiredText(
                rawName,
                "INVALID_CLIENT_NAME",
                "Informe um nome válido para o cliente."
        );

        NormalizedVehicleData vehicle = normalizeVehicle(rawVehicleName, rawPlate);

        if (name.length() > 120) {
            throw new InvalidRequestException(
                    "CLIENT_NAME_TOO_LONG",
                    "O nome deve possuir no máximo 120 caracteres."
            );
        }

        return new NormalizedClientData(
                name,
                normalizeForSearch(name),
                normalizePhone(rawPhone),
                vehicle.vehicleName(),
                normalizeForSearch(vehicle.vehicleName()),
                vehicle.plate()
        );
    }

    public NormalizedVehicleData normalizeVehicle(
            String rawVehicleName,
            String rawPlate
    ) {
        String vehicleName = normalizeRequiredText(rawVehicleName, "INVALID_VEHICLE_NAME", "Informe um veículo válido.");
        if (vehicleName.length() > 120) {
            throw new InvalidRequestException("VEHICLE_NAME_TOO_LONG", "O veículo deve possuir no máximo 120 caracteres.");
        }
        return new NormalizedVehicleData(vehicleName, normalizePlate(rawPlate));
    }

    private String normalizeRequiredText(
            String value,
            String code,
            String message
    ) {
        if (value == null) {
            throw new InvalidRequestException(code, message);
        }

        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.isBlank()) {
            throw new InvalidRequestException(code, message);
        }
        return normalized;
    }

    private String normalizePhone(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String digits = value.replaceAll("\\D", "");
        if (digits.length() != 10 && digits.length() != 11) {
            throw new InvalidRequestException(
                    "INVALID_CLIENT_PHONE",
                    "O telefone deve possuir 10 ou 11 dígitos."
            );
        }
        return digits;
    }

    private String normalizePlate(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidRequestException(
                    "INVALID_VEHICLE_PLATE",
                    "Informe uma placa válida."
            );
        }

        String plate = value
                .replaceAll("[^A-Za-z0-9]", "")
                .toUpperCase(Locale.ROOT);

        boolean valid = OLD_PLATE_PATTERN.matcher(plate).matches()
                || MERCOSUL_PLATE_PATTERN.matcher(plate).matches();

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

    public record NormalizedClientData(
            String name,
            String normalizedName,
            String phoneDigits,
            String vehicleName,
            String normalizedVehicleName,
            String plate
    ) {
    }

    public record NormalizedVehicleData(String vehicleName, String plate) {
    }
}
