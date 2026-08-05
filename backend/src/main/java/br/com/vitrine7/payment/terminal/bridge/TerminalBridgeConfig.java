package br.com.vitrine7.payment.terminal.bridge;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TerminalBridgeProperties.class)
public class TerminalBridgeConfig {
}
