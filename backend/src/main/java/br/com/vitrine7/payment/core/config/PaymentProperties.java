package br.com.vitrine7.payment.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.payment")
public class PaymentProperties {

    private boolean manualFallbackEnabled = false;

    public boolean isManualFallbackEnabled() {
        return manualFallbackEnabled;
    }

    public void setManualFallbackEnabled(
            boolean manualFallbackEnabled
    ) {
        this.manualFallbackEnabled =
                manualFallbackEnabled;
    }
}
