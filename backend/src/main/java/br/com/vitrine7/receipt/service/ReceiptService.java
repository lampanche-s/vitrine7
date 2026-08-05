package br.com.vitrine7.receipt.service;

import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.finance.config.FinanceProperties;
import br.com.vitrine7.receipt.dto.ReceiptLineResponse;
import br.com.vitrine7.receipt.dto.ReceiptOperationResponse;
import br.com.vitrine7.receipt.dto.ReceiptPaymentResponse;
import br.com.vitrine7.receipt.dto.ReceiptResponse;
import br.com.vitrine7.receipt.repository.ReceiptReadRepository;
import br.com.vitrine7.receipt.repository.ReceiptReadRepository.CheckoutReceiptRow;
import br.com.vitrine7.receipt.repository.ReceiptReadRepository.TabReceiptRow;
import br.com.vitrine7.receipt.repository.ReceiptReadRepository.WorkOrderReceiptRow;
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
    private final FinanceProperties financeProperties;

    @Transactional(readOnly = true)
    public ReceiptResponse getReceipt(
            UUID checkoutId,
            VitrineUserPrincipal principal
    ) {
        CheckoutReceiptRow checkout =
                repository.findCheckout(checkoutId);

        authorizationVerifier.verify(
                checkout.operationType(),
                principal
        );

        validateCheckoutReady(checkout);

        long paymentCount =
                repository.countReceiptPayments(checkoutId);

        if (paymentCount != 1L) {
            throw new BusinessException(
                    "RECEIPT_APPROVED_PAYMENT_REQUIRED",
                    "O recibo exige exatamente um pagamento concluido."
            );
        }

        ReceiptPaymentResponse payment =
                repository.findReceiptPayment(
                                checkoutId,
                                timeZone().getId()
                        )
                        .orElseThrow(() -> new BusinessException(
                                "RECEIPT_APPROVED_PAYMENT_REQUIRED",
                                "O recibo exige um pagamento concluido."
                        ));
        payment = localizePayment(payment);

        validatePayment(checkout, payment);

        return switch (checkout.operationType()) {
            case "BAR_COMMAND" -> tabReceipt(
                    checkout,
                    payment
            );
            case "LAVA_WORK_ORDER" -> workOrderReceipt(
                    checkout,
                    payment
            );
            default -> throw new BusinessException(
                    "RECEIPT_OPERATION_NOT_SUPPORTED",
                    "Tipo de operacao nao suportado para recibo."
            );
        };
    }

    private ReceiptResponse tabReceipt(
            CheckoutReceiptRow checkout,
            ReceiptPaymentResponse payment
    ) {
        TabReceiptRow tab =
                repository.findTab(checkout.id());

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

        List<ReceiptLineResponse> lines =
                repository.findTabLines(tab.id());
        validateLines(lines);

        return response(
                checkout,
                payment,
                new ReceiptOperationResponse(
                        checkout.operationType(),
                        tab.id(),
                        checkout.id(),
                        tab.name(),
                        tab.status(),
                        checkout.createdByUserId(),
                        checkout.createdByUserName()
                ),
                null,
                null,
                lines
        );
    }

    private ReceiptResponse workOrderReceipt(
            CheckoutReceiptRow checkout,
            ReceiptPaymentResponse payment
    ) {
        WorkOrderReceiptRow workOrder =
                repository.findWorkOrder(checkout.id());

        requireLink(
                checkout,
                workOrder.id(),
                workOrder.checkoutId(),
                "LAVA_WORK_ORDER_CHECKOUT_LINK_INVALID"
        );

        if (!"PAID".equals(workOrder.status())
                && !"COMPLETED".equals(workOrder.status())) {
            throw new BusinessException(
                    "LAVA_WORK_ORDER_NOT_PAID",
                    "A ordem de servico ainda nao esta paga para recibo."
            );
        }

        validateOperationAmounts(
                checkout,
                workOrder.subtotalCents(),
                workOrder.discountCents(),
                workOrder.totalCents()
        );

        List<ReceiptLineResponse> lines =
                repository.findWorkOrderLines(workOrder.id());
        validateLines(lines);

        return response(
                checkout,
                payment,
                new ReceiptOperationResponse(
                        checkout.operationType(),
                        workOrder.id(),
                        checkout.id(),
                        displayCustomer(workOrder.customerName()),
                        workOrder.status(),
                        checkout.createdByUserId(),
                        checkout.createdByUserName()
                ),
                workOrder.customer(),
                workOrder.vehicle(),
                lines
        );
    }

    private ReceiptResponse response(
            CheckoutReceiptRow checkout,
            ReceiptPaymentResponse payment,
            ReceiptOperationResponse operation,
            br.com.vitrine7.receipt.dto.ReceiptCustomerResponse customer,
            br.com.vitrine7.receipt.dto.ReceiptVehicleResponse vehicle,
            List<ReceiptLineResponse> lines
    ) {
        return new ReceiptResponse(
                "NON_FISCAL_THERMAL_80MM",
                TITLE,
                NOTICE,
                repository.findEstablishment(),
                operation,
                customer,
                vehicle,
                lines,
                checkout.subtotalCents(),
                checkout.discountCents(),
                checkout.totalCents(),
                payment,
                payment.approvedAt()
        );
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

    private void validatePayment(
            CheckoutReceiptRow checkout,
            ReceiptPaymentResponse payment
    ) {
        if (payment.approvedAt() == null) {
            throw new BusinessException(
                    "RECEIPT_PAYMENT_APPROVAL_DATE_REQUIRED",
                    "O pagamento aprovado nao possui data oficial."
            );
        }

        if (checkout.totalCents()
                != payment.approvedAmountCents()) {
            throw new BusinessException(
                    "RECEIPT_FINANCIAL_INTEGRITY_ERROR",
                    "O valor aprovado nao confere com o total do checkout."
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

    private void validateLines(List<ReceiptLineResponse> lines) {
        if (lines.isEmpty()) {
            throw new BusinessException(
                    "RECEIPT_OPERATION_EMPTY",
                    "A operacao nao possui linhas para recibo."
            );
        }
    }

    private String displayCustomer(String customerName) {
        if (customerName == null || customerName.isBlank()) {
            return "Cliente avulso";
        }

        return customerName;
    }

    private ZoneId timeZone() {
        String configured = financeProperties.businessTimeZone();
        if (configured == null || configured.isBlank()) {
            return ZoneId.of("America/Bahia");
        }

        return ZoneId.of(configured);
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
                        .atZoneSameInstant(
                                timeZone()
                        )
                        .toOffsetDateTime(),
                payment.cashReceivedCents(),
                payment.cashChangeCents(),
                payment.terminalProvider(),
                payment.reversedAt() == null
                        ? null
                        : payment.reversedAt()
                                .atZoneSameInstant(
                                        timeZone()
                                )
                                .toOffsetDateTime(),
                payment.reversalReason()
        );
    }
}
