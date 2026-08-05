package br.com.vitrine7.bar.tab.service;

import br.com.vitrine7.bar.tab.dto.BarTabCancellationResponse;
import br.com.vitrine7.bar.tab.dto.CancelBarTabRequest;
import br.com.vitrine7.bar.tab.entity.BarTabEntity;
import br.com.vitrine7.bar.tab.entity.BarTabStatus;
import br.com.vitrine7.bar.tab.repository.BarTabRepository;
import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.common.exception.NotFoundException;
import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class BarTabCancellationService {

    private final BarTabRepository tabRepository;
    private final Clock clock;

    @Transactional
    public CancellationResult cancel(
            Long tabId,
            CancelBarTabRequest request,
            VitrineUserPrincipal principal
    ) {
        BarTabEntity tab =
                tabRepository
                        .findByIdForUpdate(tabId)
                        .orElseThrow(() -> new NotFoundException(
                                "BAR_TAB_NOT_FOUND",
                                "Comanda nao encontrada."
                        ));

        if (tab.getStatus()
                == BarTabStatus.CANCELLED) {

            return new CancellationResult(
                    BarTabCancellationResponse.from(tab),
                    true
            );
        }

        if (tab.getStatus()
                == BarTabStatus.CLOSED) {

            throw new BusinessException(
                    "BAR_TAB_ALREADY_CLOSED",
                    "Uma comanda fechada nao pode ser cancelada."
            );
        }

        if (tab.getStatus()
                == BarTabStatus.PAYMENT_PENDING) {

            throw new BusinessException(
                    "BAR_TAB_PAYMENT_PENDING",
                    "Cancele primeiro o checkout da comanda."
            );
        }

        String reason = request.reason()
                .trim()
                .replaceAll("\\s+", " ");

        tab.cancelOpen(
                principal.getId(),
                reason,
                OffsetDateTime.now(clock)
        );

        tabRepository.flush();


        return new CancellationResult(
                BarTabCancellationResponse.from(tab),
                false
        );
    }

    public record CancellationResult(
            BarTabCancellationResponse response,
            boolean replayed
    ) {
    }
}
