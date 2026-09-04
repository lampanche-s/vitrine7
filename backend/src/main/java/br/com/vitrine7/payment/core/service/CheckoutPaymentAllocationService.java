package br.com.vitrine7.payment.core.service;

import br.com.vitrine7.checkout.entity.CheckoutSessionEntity;
import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.payment.core.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CheckoutPaymentAllocationService {

    public static final int MAX_PAYMENT_PARTS = 3;

    private final PaymentRepository paymentRepository;

    public Allocation resolve(
            CheckoutSessionEntity checkout,
            Long requestedAmountCents
    ) {
        long approved = paymentRepository.sumApprovedAmount(checkout.getId());
        long remaining = checkout.getTotalCents() - approved;

        if (remaining <= 0) {
            throw new BusinessException(
                    "CHECKOUT_ALREADY_PAID",
                    "Este checkout já está integralmente pago."
            );
        }

        long approvedParts = paymentRepository.countApprovedPayments(checkout.getId());
        if (approvedParts >= MAX_PAYMENT_PARTS) {
            throw new BusinessException(
                    "PAYMENT_PART_LIMIT_REACHED",
                    "O checkout aceita no máximo três formas de pagamento."
            );
        }

        long requested = requestedAmountCents == null
                ? remaining
                : requestedAmountCents;

        if (requested <= 0) {
            throw new BusinessException(
                    "INVALID_PAYMENT_PART_AMOUNT",
                    "O valor da forma de pagamento deve ser maior que zero."
            );
        }

        if (requested > remaining) {
            throw new BusinessException(
                    "PAYMENT_PART_EXCEEDS_REMAINING",
                    "O valor informado excede o saldo restante da comanda."
            );
        }

        return new Allocation(approved, remaining, requested);
    }

    public boolean applyApprovedPayment(
            CheckoutSessionEntity checkout,
            OffsetDateTime approvedAt
    ) {
        paymentRepository.flush();
        long approved = paymentRepository.sumApprovedAmount(checkout.getId());

        if (approved > checkout.getTotalCents()) {
            throw new BusinessException(
                    "CHECKOUT_PAYMENT_TOTAL_EXCEEDED",
                    "Os pagamentos excedem o total do checkout."
            );
        }

        if (approved == checkout.getTotalCents()) {
            checkout.markPaid(approvedAt);
            return true;
        }

        checkout.markReadyAfterPartialPayment();
        return false;
    }

    public long remaining(UUID checkoutId, long checkoutTotalCents) {
        return checkoutTotalCents - paymentRepository.sumApprovedAmount(checkoutId);
    }

    public record Allocation(
            long approvedCents,
            long remainingCents,
            long requestedCents
    ) {
    }
}
