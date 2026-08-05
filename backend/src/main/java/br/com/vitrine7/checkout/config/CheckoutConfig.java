package br.com.vitrine7.checkout.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(
        CheckoutProperties.class
)
public class CheckoutConfig {

    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }
}
