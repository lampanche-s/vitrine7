package br.com.vitrine7.checkout.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.checkout")
public class CheckoutProperties {

    private Duration draftExpiration =
            Duration.ofMinutes(30);

    public Duration getDraftExpiration() {
        return draftExpiration;
    }

    public void setDraftExpiration(
            Duration draftExpiration
    ) {
        if (draftExpiration == null
                || draftExpiration.isZero()
                || draftExpiration.isNegative()) {

            throw new IllegalArgumentException(
                    "A expiracao do checkout deve ser positiva."
            );
        }

        this.draftExpiration = draftExpiration;
    }
}
