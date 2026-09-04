package br.com.vitrine7.pagbankagent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PendingResultRecoveryTest {

    private final ObjectMapper mapper =
            PagBankAgentApplication.objectMapper();

    @TempDir
    Path tempDir;

    @Test
    void keepsResultAndResendsWithoutChargingAgain()
            throws Exception {
        Path tokenPath =
                tempDir.resolve(
                        "device-token.properties"
                );

        Path pendingPath =
                tempDir.resolve(
                        "pending-result.json"
                );

        UUID deviceId = UUID.randomUUID();
        UUID commandId = UUID.randomUUID();

        DeviceTokenStore tokenStore =
                new DeviceTokenStore(tokenPath);

        tokenStore.save(
                new DeviceToken(
                        deviceId,
                        "device-token-secret"
                )
        );

        AgentConfig config = new AgentConfig(
                URI.create("http://127.0.0.1"),
                tokenPath,
                null,
                "LOCAL",
                "test-agent",
                "PAGBANK-TEST",
                PaymentTerminalDriverMode.SIMULATED,
                PaymentOutcomeMode.APPROVED,
                null,
                null,
                null,
                "Vitrine 7",
                "test",
                Duration.ofSeconds(60),
                0
        );

        BridgeDtos.CommandDelivery delivery =
                new BridgeDtos.CommandDelivery(
                        commandId,
                        UUID.randomUUID(),
                        "INITIATE_PAYMENT",
                        mapper.createObjectNode()
                                .put(
                                        "amountCents",
                                        1_290
                                )
                                .put(
                                        "paymentMethod",
                                        "CREDIT_CARD"
                                )
                                .put(
                                        "providerCode",
                                        "PAGBANK"
                                ),
                        OffsetDateTime.now()
                                .plusMinutes(1),
                        1
                );

        RecordingBridgeClient bridgeClient =
                new RecordingBridgeClient(
                        config,
                        mapper,
                        delivery
                );

        AtomicInteger paymentExecutions =
                new AtomicInteger();

        PaymentTerminalDriver driver = command -> {
            paymentExecutions.incrementAndGet();

            return Optional.of(
                    new BridgeDtos.ResultRequest(
                            ProviderPaymentStatus.APPROVED,
                            "TX-123",
                            command.commandId().toString(),
                            "AUTH-123",
                            null,
                            null,
                            mapper.createObjectNode()
                                    .put(
                                            "simulated",
                                            true
                                    )
                    )
            );
        };

        try (PagBankAgentRunner runner =
                new PagBankAgentRunner(
                        config,
                        tokenStore,
                        new PendingResultStore(
                                pendingPath,
                                mapper
                        ),
                        bridgeClient,
                        driver,
                        new PaymentTerminalCapabilities(
                                true,
                                true,
                                false,
                                true,
                                "SIMULATED"
                        ),
                        mapper
                )) {

            runner.pairIfNeeded();

            assertThrows(
                    IOException.class,
                    runner::pollAndProcessOnce
            );

            assertTrue(Files.exists(pendingPath));
            assertEquals(1, paymentExecutions.get());
            assertEquals(1, bridgeClient.resultAttempts());

            Optional<BridgeDtos.ResultResponse> recovered =
                    runner.pollAndProcessOnce();

            assertTrue(recovered.isPresent());
            assertFalse(Files.exists(pendingPath));
            assertEquals(1, paymentExecutions.get());
            assertEquals(2, bridgeClient.resultAttempts());
        }
    }

    private static final class RecordingBridgeClient
            extends BridgeClient {

        private final BridgeDtos.CommandDelivery delivery;

        private boolean delivered;
        private int resultAttempts;

        private RecordingBridgeClient(
                AgentConfig config,
                ObjectMapper mapper,
                BridgeDtos.CommandDelivery delivery
        ) {
            super(
                    config,
                    HttpClient.newHttpClient(),
                    mapper
            );

            this.delivery = delivery;
        }

        @Override
        public Optional<BridgeDtos.CommandDelivery>
        nextCommand(
                DeviceToken token,
                int waitSeconds
        ) {
            if (delivered) {
                return Optional.empty();
            }

            delivered = true;

            return Optional.of(delivery);
        }

        @Override
        public BridgeDtos.Acknowledgement ack(
                DeviceToken token,
                UUID commandId
        ) {
            return new BridgeDtos.Acknowledgement(
                    commandId,
                    "ACKNOWLEDGED",
                    OffsetDateTime.now()
            );
        }

        @Override
        public BridgeDtos.ResultResponse result(
                DeviceToken token,
                UUID commandId,
                BridgeDtos.ResultRequest request
        ) throws IOException {
            resultAttempts++;

            if (resultAttempts == 1) {
                throw new IOException(
                        "Falha simulada depois da aprovacao."
                );
            }

            return new BridgeDtos.ResultResponse(
                    commandId,
                    "COMPLETED",
                    true
            );
        }

        int resultAttempts() {
            return resultAttempts;
        }
    }
}
