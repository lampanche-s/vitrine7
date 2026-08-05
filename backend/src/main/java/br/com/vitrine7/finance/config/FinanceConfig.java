package br.com.vitrine7.finance.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FinanceProperties.class)
public class FinanceConfig {
}
