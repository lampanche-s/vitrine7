package br.com.vitrine7.checkout.service;

import br.com.vitrine7.checkout.repository.CheckoutSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class CheckoutExpirationService {

    private final CheckoutSessionRepository checkoutRepository;
    private final Clock clock;

    @Transactional
    public int expireStaleSessions() {
        return checkoutRepository.expireStaleSessions(
                OffsetDateTime.now(clock)
        );
    }
}
