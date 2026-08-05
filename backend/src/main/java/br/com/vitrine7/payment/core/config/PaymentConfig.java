package br.com.vitrine7.payment.core.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(
        PaymentProperties.class
)
public class PaymentConfig {
}
