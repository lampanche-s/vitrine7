package br.com.vitrine7.print.service;

import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.common.exception.NotFoundException;
import br.com.vitrine7.print.dto.PrintJobDtos;
import br.com.vitrine7.print.repository.PrintJobRepository;
import br.com.vitrine7.receipt.dto.ReceiptResponse;
import br.com.vitrine7.receipt.service.ReceiptService;
import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class PrintJobService {

    private final PrintJobRepository repository;
    private final ReceiptService receiptService;
    private final ReceiptTextRenderer textRenderer;
    private final Clock clock;

    public PrintJobService(
            PrintJobRepository repository,
            ReceiptService receiptService,
            ReceiptTextRenderer textRenderer,
            Clock clock
    ) {
        this.repository = repository;
        this.receiptService = receiptService;
        this.textRenderer = textRenderer;
        this.clock = clock;
    }

    @Transactional
    public PrintJobDtos.Created create(
            UUID checkoutId,
            VitrineUserPrincipal principal
    ) {
        PrintJobDtos.Created active = repository
                .findActiveByCheckoutId(checkoutId)
                .orElse(null);

        if (active != null) {
            return active;
        }

        ReceiptResponse receipt = receiptService.getReceipt(checkoutId, principal);
        UUID id = UUID.randomUUID();

        repository.create(
                id,
                checkoutId,
                principal.getId(),
                textRenderer.render(receipt)
        );

        return new PrintJobDtos.Created(id, "PENDING");
    }

    @Transactional(readOnly = true)
    public PrintJobDtos.Status status(UUID id) {
        return repository.findStatus(id)
                .orElseThrow(() -> new NotFoundException(
                        "PRINT_JOB_NOT_FOUND",
                        "Impressao nao encontrada."
                ));
    }

    @Transactional
    public PrintJobDtos.Delivery reserveNext() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        return repository.reserveNext(now).orElse(null);
    }

    @Transactional
    public PrintJobDtos.ResultResponse complete(
            UUID id,
            PrintJobDtos.ResultRequest request
    ) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        boolean updated = repository.complete(
                id,
                request.success(),
                request.errorMessage(),
                now
        );

        if (!updated) {
            throw new BusinessException(
                    "PRINT_JOB_NOT_PRINTING",
                    "A impressao nao esta aguardando resultado do agente."
            );
        }

        return new PrintJobDtos.ResultResponse(
                id,
                request.success() ? "PRINTED" : "FAILED"
        );
    }
}
