package br.com.vitrine7.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record BusinessProperties(
        String businessTimeZone,
        Establishment establishment
) {

    public record Establishment(
            String name,
            String document,
            String phone,
            String address
    ) {
    }
}
