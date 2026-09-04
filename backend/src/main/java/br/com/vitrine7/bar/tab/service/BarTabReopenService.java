package br.com.vitrine7.bar.tab.service;

import br.com.vitrine7.bar.tab.dto.BarTabResponse;
import br.com.vitrine7.bar.tab.entity.BarTabEntity;
import br.com.vitrine7.bar.tab.entity.BarTabLineEntity;
import br.com.vitrine7.bar.tab.entity.BarTabStatus;
import br.com.vitrine7.bar.tab.entity.BarTabClosureType;
import br.com.vitrine7.bar.tab.repository.BarTabLineRepository;
import br.com.vitrine7.bar.tab.repository.BarTabRepository;
import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.common.exception.NotFoundException;
import br.com.vitrine7.payment.core.entity.PaymentEntity;
import br.com.vitrine7.payment.core.repository.PaymentRepository;
import br.com.vitrine7.print.repository.PrintJobRepository;
import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BarTabReopenService {

    private static final String SUPERSEDE_REASON =
            "Pagamento substituído após retomada da comanda.";

    private final BarTabRepository tabRepository;
    private final BarTabLineRepository lineRepository;
    private final BarTabStockService stockService;
    private final PaymentRepository paymentRepository;
    private final PrintJobRepository printJobRepository;
    private final BarTabService tabService;
    private final Clock clock;

    @Transactional
    public BarTabResponse reopen(
            Long tabId,
            VitrineUserPrincipal principal
    ) {
        BarTabEntity tab = tabRepository.findByIdForUpdate(tabId)
                .orElseThrow(() -> new NotFoundException(
                        "BAR_TAB_NOT_FOUND",
                        "Comanda não encontrada."
                ));

        if (tab.getStatus() != BarTabStatus.CLOSED || tab.getClosedAt() == null) {
            throw new BusinessException(
                    "BAR_TAB_CANNOT_BE_REOPENED",
                    "Somente uma comanda fechada pode ser retomada."
            );
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        OffsetDateTime deadline = tab.getClosedAt().plusHours(1);
        if (now.isAfter(deadline)) {
            throw new BusinessException(
                    "BAR_TAB_REOPEN_WINDOW_EXPIRED",
                    "O prazo de uma hora para retomar esta comanda expirou."
            );
        }

        List<BarTabLineEntity> lines = lineRepository.findAllByTabIdOrderByIdAsc(tabId);
        stockService.restore(lines);

        if (tab.getClosureType() == BarTabClosureType.VOUCHER) {
            tab.reopenClosedVoucher(principal.getId(), now);
            tabRepository.flush();
            return tabService.findById(tabId);
        }

        if (tab.getClosureType() != BarTabClosureType.PAYMENT || tab.getCheckoutSessionId() == null) {
            throw new BusinessException("BAR_TAB_CLOSURE_INVALID", "O tipo de encerramento da comanda é inválido.");
        }

        var oldCheckoutId = tab.getCheckoutSessionId();
        List<PaymentEntity> approvedPayments =
                paymentRepository.findApprovedByCheckoutForUpdate(oldCheckoutId);
        if (approvedPayments.isEmpty()) {
            throw new BusinessException(
                    "BAR_TAB_APPROVED_PAYMENT_REQUIRED",
                    "A comanda não possui pagamento aprovado para substituição."
            );
        }

        for (PaymentEntity payment : approvedPayments) {
            payment.markSuperseded(
                    principal.getId(),
                    SUPERSEDE_REASON,
                    now
            );
        }

        printJobRepository.cancelActiveForCheckout(oldCheckoutId, now);
        paymentRepository.flush();
        tab.reopenClosedPayment(principal.getId(), now);
        tabRepository.flush();

        return tabService.findById(tabId);
    }
}
