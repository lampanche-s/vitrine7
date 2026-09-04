package br.com.vitrine7.payment.terminal.bridge;

import br.com.vitrine7.checkout.service.CheckoutFinalizationService;
import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.payment.core.entity.PaymentMethod;
import br.com.vitrine7.payment.terminal.adapter.ProviderPaymentResult;
import br.com.vitrine7.payment.terminal.bridge.dto.TerminalCommandDtos;
import br.com.vitrine7.payment.terminal.entity.PaymentTerminalMode;
import br.com.vitrine7.payment.terminal.entity.PaymentTerminalProvider;
import br.com.vitrine7.payment.terminal.entity.PaymentTerminalTransactionEntity;
import br.com.vitrine7.payment.terminal.provider.PaymentProviderCode;
import br.com.vitrine7.payment.terminal.provider.PaymentProviderEnvironment;
import br.com.vitrine7.payment.terminal.provider.ProviderPaymentStatus;
import br.com.vitrine7.payment.terminal.repository.PaymentTerminalTransactionRepository;
import br.com.vitrine7.payment.terminal.service.TerminalPaymentCompletionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TerminalCommandQueueServiceTest {

    private final TerminalCommandRepository commandRepository =
            mock(TerminalCommandRepository.class);
    private final TerminalDeviceRepository deviceRepository =
            mock(TerminalDeviceRepository.class);
    private final PaymentTerminalTransactionRepository transactionRepository =
            mock(PaymentTerminalTransactionRepository.class);
    private final TerminalPaymentCompletionService completionService =
            mock(TerminalPaymentCompletionService.class);
    private final CheckoutFinalizationService finalizationService =
            mock(CheckoutFinalizationService.class);
    private final TransactionTemplate transactionTemplate =
            mock(TransactionTemplate.class);
    private final ObjectMapper mapper = new ObjectMapper();

    private TerminalCommandQueueService service;
    private UUID commandId;
    private UUID deviceId;
    private UUID transactionId;
    private UUID checkoutId;
    private UUID paymentId;

    @BeforeEach
    void setUp() {
        service = new TerminalCommandQueueService(
                commandRepository,
                deviceRepository,
                transactionRepository,
                completionService,
                finalizationService,
                new TerminalBridgeProperties(
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(30),
                        Duration.ofMinutes(1)
                ),
                mapper,
                Clock.fixed(
                        java.time.Instant.parse("2026-07-19T13:00:00Z"),
                        ZoneOffset.UTC
                ),
                transactionTemplate
        );
        commandId = UUID.randomUUID();
        deviceId = UUID.randomUUID();
        transactionId = UUID.randomUUID();
        checkoutId = UUID.randomUUID();
        paymentId = UUID.randomUUID();

        doAnswer(invocation -> {
            Consumer<TransactionStatus> consumer = invocation.getArgument(0);
            consumer.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    @Test
    void approvedResultCompletesTransactionAndFinalizesCheckoutWhenSupported() {
        arrangeCommand();

        service.submitResult(
                commandId,
                deviceId,
                request(
                        ProviderPaymentStatus.APPROVED,
                        "PAG-APPROVED",
                        null,
                        null
                )
        );

        ArgumentCaptor<ProviderPaymentResult> result =
                ArgumentCaptor.forClass(ProviderPaymentResult.class);
        verify(completionService).complete(
                eq(checkoutId),
                eq(paymentId),
                result.capture(),
                eq(10L)
        );
        assertEquals(ProviderPaymentStatus.APPROVED, result.getValue().status());
        verify(finalizationService)
                .finalizeCheckoutIfSupported(checkoutId, 10L);

        InOrder processingOrder =
                inOrder(
                        completionService,
                        commandRepository
                );

        processingOrder.verify(completionService)
                .complete(
                        eq(checkoutId),
                        eq(paymentId),
                        any(),
                        eq(10L)
                );

        processingOrder.verify(commandRepository)
                .finish(
                        eq(commandId),
                        eq(deviceId),
                        eq("COMPLETED"),
                        any(),
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    void declinedResultCompletesTransactionAsDeclined() {
        arrangeCommand();

        service.submitResult(
                commandId,
                deviceId,
                request(
                        ProviderPaymentStatus.DECLINED,
                        "PAG-DECLINED",
                        "51",
                        "Transacao recusada."
                )
        );

        ArgumentCaptor<ProviderPaymentResult> result =
                ArgumentCaptor.forClass(ProviderPaymentResult.class);
        verify(completionService).complete(
                eq(checkoutId),
                eq(paymentId),
                result.capture(),
                eq(10L)
        );
        assertEquals(ProviderPaymentStatus.DECLINED, result.getValue().status());
        assertEquals("51", result.getValue().failureCode());
        verify(finalizationService, never())
                .finalizeCheckoutIfSupported(any(), any());
    }

    @Test
    void errorResultFailsCommunication() {
        arrangeCommand();

        service.submitResult(
                commandId,
                deviceId,
                request(
                        ProviderPaymentStatus.ERROR,
                        null,
                        "PAGBANK_AGENT_ERROR",
                        "Falha simulada."
                )
        );

        verify(completionService).failCommunication(
                checkoutId,
                paymentId,
                "PAGBANK_AGENT_ERROR",
                "Falha simulada."
        );
        verify(completionService, never())
                .complete(any(), any(), any(), any());
        verify(finalizationService, never())
                .finalizeCheckoutIfSupported(any(), any());
    }

    @Test
    void identicalCompletedReplayRepairsFinancialFlow() {
        TerminalCommandDtos.ResultRequest approved =
                request(
                        ProviderPaymentStatus.APPROVED,
                        "PAG-APPROVED",
                        null,
                        null
                );

        ObjectNode storedResult =
                mapper.createObjectNode();

        storedResult.put(
                "status",
                ProviderPaymentStatus.APPROVED.name()
        );

        storedResult.put(
                "providerReference",
                "PAG-APPROVED"
        );

        storedResult.put(
                "providerRequestId",
                commandId.toString()
        );

        storedResult.put(
                "authorizationCode",
                "123456"
        );

        storedResult.putNull("failureCode");
        storedResult.putNull("failureMessage");

        storedResult.set(
                "metadata",
                approved.metadata().deepCopy()
        );

        when(commandRepository.find(commandId))
                .thenReturn(
                        snapshot(
                                "COMPLETED",
                                storedResult
                        )
                );

        when(transactionRepository.findById(transactionId))
                .thenReturn(
                        Optional.of(transaction())
                );

        TerminalCommandDtos.ResultResponse response =
                service.submitResult(
                        commandId,
                        deviceId,
                        approved
                );

        assertEquals(true, response.replayed());

        verify(completionService).complete(
                eq(checkoutId),
                eq(paymentId),
                any(),
                eq(10L)
        );

        verify(finalizationService)
                .finalizeCheckoutIfSupported(
                        checkoutId,
                        10L
                );

        verify(commandRepository, never())
                .finish(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    void approvedResultAfterExpirationIsReconciled() {
        TerminalCommandRepository.CommandSnapshot expired =
                new TerminalCommandRepository.CommandSnapshot(
                        commandId,
                        deviceId,
                        transactionId,
                        "INITIATE_PAYMENT",
                        "EXPIRED",
                        mapper.createObjectNode(),
                        OffsetDateTime.parse(
                                "2026-07-19T12:59:59Z"
                        ),
                        "PAYMENT_TERMINAL_COMMAND_EXPIRED",
                        "Comando expirado."
                );

        TerminalCommandRepository.CommandSnapshot completed =
                snapshot(
                        "COMPLETED",
                        mapper.createObjectNode()
                );

        when(commandRepository.find(commandId))
                .thenReturn(expired)
                .thenReturn(completed);

        when(commandRepository.finish(
                eq(commandId),
                eq(deviceId),
                eq("COMPLETED"),
                any(),
                any(),
                any(),
                any()
        )).thenReturn(1);

        when(transactionRepository.findById(transactionId))
                .thenReturn(
                        Optional.of(transaction())
                );

        TerminalCommandDtos.ResultResponse response =
                service.submitResult(
                        commandId,
                        deviceId,
                        request(
                                ProviderPaymentStatus.APPROVED,
                                "PAG-LATE-APPROVED",
                                null,
                                null
                        )
                );

        assertEquals(
                "COMPLETED",
                response.commandStatus()
        );

        verify(completionService).complete(
                eq(checkoutId),
                eq(paymentId),
                any(),
                eq(10L)
        );

        verify(finalizationService)
                .finalizeCheckoutIfSupported(
                        checkoutId,
                        10L
                );
    }

    @Test
    void processingResultAfterExpirationIsRejected() {
        TerminalCommandRepository.CommandSnapshot expired =
                new TerminalCommandRepository.CommandSnapshot(
                        commandId,
                        deviceId,
                        transactionId,
                        "INITIATE_PAYMENT",
                        "EXPIRED",
                        mapper.createObjectNode(),
                        OffsetDateTime.parse(
                                "2026-07-19T12:59:59Z"
                        ),
                        "PAYMENT_TERMINAL_COMMAND_EXPIRED",
                        "Comando expirado."
                );

        when(commandRepository.find(commandId))
                .thenReturn(expired);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> service.submitResult(
                                commandId,
                                deviceId,
                                request(
                                        ProviderPaymentStatus.PROCESSING,
                                        null,
                                        null,
                                        null
                                )
                        )
                );

        assertEquals(
                "PAYMENT_TERMINAL_COMMAND_EXPIRED",
                exception.getCode()
        );

        verifyNoInteractions(
                completionService,
                finalizationService
        );
    }

    @Test
    void initiationUsesTransactionSpecificUserReference() {
        UUID localPaymentId =
                UUID.randomUUID();

        UUID localTransactionId =
                UUID.fromString(
                        "12345678-90ab-cdef-1234-567890abcdef"
                );

        UUID profileId =
                UUID.randomUUID();

        TerminalCommandRepository.CommandTransaction transaction =
                new TerminalCommandRepository.CommandTransaction(
                        localTransactionId,
                        localPaymentId,
                        checkoutId,
                        UUID.randomUUID(),
                        "PAGBANK",
                        "LOCAL",
                        1L,
                        profileId,
                        "CREDIT_CARD",
                        1_290L,
                        10L,
                        "ESTABELECIMENTO",
                        "PAGBANK-LOCAL"
                );

        when(commandRepository.findTransactionByPayment(
                localPaymentId
        )).thenReturn(transaction);

        when(commandRepository.findByTransaction(
                localTransactionId
        )).thenReturn(null);

        when(deviceRepository.selectAvailable(
                eq(PaymentProviderCode.PAGBANK),
                eq(profileId),
                eq("PAGBANK-LOCAL"),
                eq("CREDIT_CARD"),
                any(OffsetDateTime.class)
        )).thenReturn(
                new TerminalDeviceRepository.DeviceSelection(
                        deviceId,
                        PaymentProviderCode.PAGBANK
                )
        );

        ArgumentCaptor<tools.jackson.databind.JsonNode>
                payloadCaptor =
                ArgumentCaptor.forClass(
                        tools.jackson.databind.JsonNode.class
                );

        service.createInitiation(localPaymentId);

        verify(commandRepository).create(
                any(UUID.class),
                eq(deviceId),
                eq(transaction),
                payloadCaptor.capture(),
                any(OffsetDateTime.class)
        );

        assertEquals(
                "1234567890",
                payloadCaptor
                        .getValue()
                        .path("userReference")
                        .asText()
        );

        assertEquals(
                false,
                payloadCaptor
                        .getValue()
                        .has("merchantReference")
        );
    }

    @Test
    void matchingQueryApprovalCompletesPayment() {
        String expectedReference =
                TerminalCommandQueueService
                        .paymentUserReference(
                                transactionId
                        );

        TerminalCommandRepository.CommandSnapshot before =
                new TerminalCommandRepository.CommandSnapshot(
                        commandId,
                        deviceId,
                        transactionId,
                        "QUERY_PAYMENT",
                        "ACKNOWLEDGED",
                        mapper.createObjectNode(),
                        OffsetDateTime.now()
                                .plusMinutes(1),
                        null,
                        null
                );

        TerminalCommandRepository.CommandSnapshot after =
                new TerminalCommandRepository.CommandSnapshot(
                        commandId,
                        deviceId,
                        transactionId,
                        "QUERY_PAYMENT",
                        "COMPLETED",
                        mapper.createObjectNode(),
                        OffsetDateTime.now()
                                .plusMinutes(1),
                        null,
                        null
                );

        when(commandRepository.find(commandId))
                .thenReturn(before)
                .thenReturn(after);

        when(commandRepository.finish(
                eq(commandId),
                eq(deviceId),
                eq("COMPLETED"),
                any(),
                any(),
                any(),
                any()
        )).thenReturn(1);

        when(transactionRepository.findById(
                transactionId
        )).thenReturn(
                Optional.of(transaction())
        );

        ObjectNode metadata =
                mapper.createObjectNode();

        metadata.put(
                "userReference",
                expectedReference
        );

        service.submitResult(
                commandId,
                deviceId,
                new TerminalCommandDtos.ResultRequest(
                        ProviderPaymentStatus.APPROVED,
                        "PAGBANK-TX-123",
                        commandId.toString(),
                        "NSU-123",
                        null,
                        null,
                        metadata
                )
        );

        verify(completionService).complete(
                eq(checkoutId),
                eq(paymentId),
                any(),
                eq(10L)
        );

        verify(finalizationService)
                .finalizeCheckoutIfSupported(
                        checkoutId,
                        10L
                );

        verify(commandRepository)
                .mirrorTransactionDelivery(
                        transactionId,
                        "COMPLETED"
                );
    }

    @Test
    void mismatchedQueryApprovalDoesNotCompletePayment() {
        TerminalCommandRepository.CommandSnapshot before =
                new TerminalCommandRepository.CommandSnapshot(
                        commandId,
                        deviceId,
                        transactionId,
                        "QUERY_PAYMENT",
                        "ACKNOWLEDGED",
                        mapper.createObjectNode(),
                        OffsetDateTime.now()
                                .plusMinutes(1),
                        null,
                        null
                );

        TerminalCommandRepository.CommandSnapshot after =
                new TerminalCommandRepository.CommandSnapshot(
                        commandId,
                        deviceId,
                        transactionId,
                        "QUERY_PAYMENT",
                        "FAILED",
                        mapper.createObjectNode(),
                        OffsetDateTime.now()
                                .plusMinutes(1),
                        null,
                        null
                );

        when(commandRepository.find(commandId))
                .thenReturn(before)
                .thenReturn(after);

        when(commandRepository.finish(
                eq(commandId),
                eq(deviceId),
                eq("FAILED"),
                any(),
                any(),
                any(),
                any()
        )).thenReturn(1);

        ObjectNode metadata =
                mapper.createObjectNode();

        metadata.put(
                "userReference",
                "OUTRAVENDA"
        );

        service.submitResult(
                commandId,
                deviceId,
                new TerminalCommandDtos.ResultRequest(
                        ProviderPaymentStatus.APPROVED,
                        "PAGBANK-OUTRA-TX",
                        commandId.toString(),
                        "NSU-OUTRO",
                        null,
                        null,
                        metadata
                )
        );

        verifyNoInteractions(
                completionService,
                finalizationService
        );

        verify(
                commandRepository,
                never()
        ).mirrorTransactionDelivery(
                any(),
                any()
        );
    }

    private void arrangeCommand() {
        TerminalCommandRepository.CommandSnapshot before =
                snapshot("ACKNOWLEDGED", mapper.createObjectNode());
        TerminalCommandRepository.CommandSnapshot after =
                snapshot("COMPLETED", mapper.createObjectNode());
        when(commandRepository.find(commandId))
                .thenReturn(before)
                .thenReturn(after);
        when(commandRepository.finish(
                eq(commandId),
                eq(deviceId),
                any(),
                any(),
                any(),
                any(),
                any()
        )).thenReturn(1);
        when(transactionRepository.findById(transactionId))
                .thenReturn(Optional.of(transaction()));
    }

    private TerminalCommandRepository.CommandSnapshot snapshot(
            String status,
            ObjectNode result
    ) {
        return new TerminalCommandRepository.CommandSnapshot(
                commandId,
                deviceId,
                transactionId,
                "INITIATE_PAYMENT",
                status,
                result,
                OffsetDateTime.now().plusMinutes(1),
                null,
                null
        );
    }

    private PaymentTerminalTransactionEntity transaction() {
        return PaymentTerminalTransactionEntity.sent(
                paymentId,
                checkoutId,
                UUID.randomUUID(),
                "a".repeat(64),
                PaymentTerminalProvider.PAGBANK,
                PaymentTerminalMode.REAL,
                PaymentMethod.CREDIT_CARD,
                1290L,
                10L,
                OffsetDateTime.now(),
                UUID.randomUUID(),
                PaymentProviderCode.PAGBANK,
                PaymentProviderEnvironment.LOCAL,
                1L
        );
    }

    private TerminalCommandDtos.ResultRequest request(
            ProviderPaymentStatus status,
            String providerReference,
            String failureCode,
            String failureMessage
    ) {
        ObjectNode metadata = mapper.createObjectNode();
        metadata.put("terminalReference", "PAGBANK-LOCAL-DEV");
        metadata.put(
                "userReference",
                "1234567890"
        );
        return new TerminalCommandDtos.ResultRequest(
                status,
                providerReference,
                commandId.toString(),
                status == ProviderPaymentStatus.APPROVED ? "123456" : null,
                failureCode,
                failureMessage,
                metadata
        );
    }
}
