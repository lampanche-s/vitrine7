package br.com.vitrine7.finance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record FinanceProperties(
        String businessTimeZone
) {
}
