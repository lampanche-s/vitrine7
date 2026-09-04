package br.com.vitrine7.bar.tab.service;

import br.com.vitrine7.bar.tab.entity.BarTabEntity;
import br.com.vitrine7.bar.tab.repository.BarTabRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BarTabCreationService {

    private final BarTabRepository tabRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BarTabEntity create(
            String name,
            String normalizedName,
            UUID idempotencyKey,
            String requestFingerprint,
            Long clientId,
            Long employeeId,
            Long actorUserId
    ) {
        return tabRepository.saveAndFlush(
                BarTabEntity.open(
                        name,
                        normalizedName,
                        idempotencyKey,
                        requestFingerprint,
                        clientId,
                        employeeId,
                        actorUserId
                )
        );
    }
}
