package br.com.vitrine7.receipt.service;

import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.receipt.dto.ReceiptEstablishmentResponse;
import br.com.vitrine7.receipt.dto.ReceiptLineResponse;
import br.com.vitrine7.receipt.dto.ReceiptOperationResponse;
import br.com.vitrine7.receipt.dto.ReceiptPaymentResponse;
import br.com.vitrine7.receipt.dto.ReceiptResponse;
import br.com.vitrine7.receipt.repository.ReceiptReadRepository;
import br.com.vitrine7.receipt.repository.ReceiptReadRepository.CheckoutReceiptRow;
import br.com.vitrine7.receipt.repository.ReceiptReadRepository.TabReceiptRow;
import br.com.vitrine7.common.config.BusinessProperties;
import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReceiptService {

    private static final String TITLE = "COMPROVANTE NAO FISCAL";
    private static final String NOTICE = "ESTE DOCUMENTO NAO E FISCAL";

    private final ReceiptReadRepository repository;
    private final ReceiptAuthorizationVerifier authorizationVerifier;
    private final BusinessProperties businessProperties;

    @Transactional(readOnly = true)
    public ReceiptResponse getReceipt(
            UUID checkoutId,
            VitrineUserPrincipal principal
    ) {
        CheckoutReceiptRow checkout = repository.findCheckout(checkoutId);
        authorizationVerifier.verify(checkout.operationType(), principal);
        validateCheckoutReady(checkout);

        List<ReceiptPaymentResponse> payments = repository.findReceiptPayments(checkoutId)
                .stream()
                .map(this::localizePayment)
                .toList();

        if (payments.isEmpty()) {
            throw new BusinessException(
                    "RECEIPT_APPROVED_PAYMENT_REQUIRED",
                    "O recibo exige pelo menos um pagamento concluído."
            );
        }

        validatePayments(checkout, payments);

        if (!"BAR_COMMAND".equals(checkout.operationType())) {
            throw new BusinessException(
                    "RECEIPT_OPERATION_NOT_SUPPORTED",
                    "Tipo de operacao nao suportado para recibo."
            );
        }

        return tabReceipt(checkout, payments);
    }

    private ReceiptResponse tabReceipt(
            CheckoutReceiptRow checkout,
            List<ReceiptPaymentResponse> payments
    ) {
        TabReceiptRow tab = repository.findTab(checkout.id());

        requireLink(
                checkout,
                tab.id(),
                tab.checkoutId(),
                "BAR_TAB_CHECKOUT_LINK_INVALID"
        );

        if (!"CLOSED".equals(tab.status())) {
            throw new BusinessException(
                    "BAR_TAB_NOT_CLOSED",
                    "A comanda ainda nao esta fechada para recibo."
            );
        }

        validateOperationAmounts(
                checkout,
                tab.subtotalCents(),
                tab.discountCents(),
                tab.totalCents()
        );

        List<ReceiptLineResponse> lines = repository.findTabLines(tab.id());
        if (lines.isEmpty()) {
            throw new BusinessException(
                    "RECEIPT_OPERATION_EMPTY",
                    "A operacao nao possui linhas para recibo."
            );
        }

        return new ReceiptResponse(
                "NON_FISCAL_THERMAL_80MM",
                TITLE,
                NOTICE,
                establishment(),
                new ReceiptOperationResponse(
                        checkout.operationType(),
                        tab.id(),
                        checkout.id(),
                        tab.name(),
                        tab.status(),
                        checkout.createdByUserId(),
                        checkout.createdByUserName(),
                        tab.vehicleName(),
                        tab.vehiclePlate()
                ),
                lines,
                checkout.subtotalCents(),
                checkout.discountCents(),
                checkout.totalCents(),
                payments,
                payments.stream()
                        .map(ReceiptPaymentResponse::approvedAt)
                        .filter(java.util.Objects::nonNull)
                        .max(java.time.OffsetDateTime::compareTo)
                        .orElseThrow()
        );
    }


    private ReceiptEstablishmentResponse establishment() {
        BusinessProperties.Establishment configured =
                businessProperties.establishment();

        if (configured == null
                || configured.name() == null
                || configured.name().isBlank()) {
            throw new IllegalStateException(
                    "APP_ESTABLISHMENT_NAME deve estar configurado."
            );
        }

        return new ReceiptEstablishmentResponse(
                configured.name().trim(),
                blankToNull(configured.document()),
                blankToNull(configured.phone()),
                blankToNull(configured.address())
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }

    private void validateCheckoutReady(CheckoutReceiptRow checkout) {
        if (!"FINALIZED".equals(checkout.status())) {
            throw new BusinessException(
                    "RECEIPT_CHECKOUT_NOT_READY",
                    "O checkout ainda nao esta finalizado para recibo."
            );
        }

        if (checkout.sourceId() == null) {
            throw new BusinessException(
                    "RECEIPT_OPERATION_LINK_INVALID",
                    "O checkout nao esta vinculado a uma operacao."
            );
        }
    }

    private void validatePayments(
            CheckoutReceiptRow checkout,
            List<ReceiptPaymentResponse> payments
    ) {
        long approvedTotal = 0L;
        String status = null;

        for (ReceiptPaymentResponse payment : payments) {
            if (payment.approvedAt() == null) {
                throw new BusinessException(
                        "RECEIPT_PAYMENT_APPROVAL_DATE_REQUIRED",
                        "Um pagamento aprovado não possui data oficial."
                );
            }

            approvedTotal = Math.addExact(
                    approvedTotal,
                    payment.approvedAmountCents()
            );

            if (status == null) {
                status = payment.status();
            } else if (!status.equals(payment.status())) {
                throw new BusinessException(
                        "RECEIPT_PAYMENT_STATUS_INCONSISTENT",
                        "As partes do pagamento possuem estados incompatíveis."
                );
            }
        }

        if (checkout.totalCents() != approvedTotal) {
            throw new BusinessException(
                    "RECEIPT_FINANCIAL_INTEGRITY_ERROR",
                    "A soma dos pagamentos não confere com o total do checkout."
            );
        }
    }

    private void validateOperationAmounts(
            CheckoutReceiptRow checkout,
            long subtotalCents,
            long discountCents,
            long totalCents
    ) {
        if (checkout.subtotalCents() != subtotalCents
                || checkout.discountCents() != discountCents
                || checkout.totalCents() != totalCents) {
            throw new BusinessException(
                    "RECEIPT_FINANCIAL_INTEGRITY_ERROR",
                    "Os valores da operacao nao conferem com o checkout."
            );
        }
    }

    private void requireLink(
            CheckoutReceiptRow checkout,
            long operationId,
            UUID operationCheckoutId,
            String code
    ) {
        if (!checkout.id().equals(operationCheckoutId)
                || checkout.sourceId() == null
                || checkout.sourceId() != operationId) {
            throw new BusinessException(
                    code,
                    "O vinculo entre a operacao e o checkout e invalido."
            );
        }
    }

    private ZoneId timeZone() {
        String configured = businessProperties.businessTimeZone();
        return configured == null || configured.isBlank()
                ? ZoneId.of("America/Bahia")
                : ZoneId.of(configured);
    }

    private ReceiptPaymentResponse localizePayment(
            ReceiptPaymentResponse payment
    ) {
        if (payment.approvedAt() == null) {
            return payment;
        }

        return new ReceiptPaymentResponse(
                payment.paymentId(),
                payment.method(),
                payment.processingMode(),
                payment.status(),
                payment.approvedAmountCents(),
                payment.approvedAt()
                        .atZoneSameInstant(timeZone())
                        .toOffsetDateTime(),
                payment.cashReceivedCents(),
                payment.cashChangeCents(),
                payment.terminalProvider(),
                payment.reversedAt() == null
                        ? null
                        : payment.reversedAt()
                                .atZoneSameInstant(timeZone())
                                .toOffsetDateTime(),
                payment.reversalReason()
        );
    }
}
