package br.com.vitrine7.pagbankagent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PagBankAgentRunner implements AutoCloseable {

    private static final Logger LOGGER =
            Logger.getLogger(
                    PagBankAgentRunner.class.getName()
            );

    private static final long RETRY_DELAY_MILLIS =
            3_000L;

    private final AgentConfig config;
    private final DeviceTokenStore tokenStore;
    private final PendingResultStore pendingResultStore;
    private final BridgeClient bridgeClient;
    private final PaymentTerminalDriver driver;
    private final PaymentTerminalCapabilities capabilities;
    private final ObjectMapper mapper;
    private final AtomicBoolean running =
            new AtomicBoolean(true);

    private DeviceToken currentToken;
    private Instant nextHeartbeatAt = Instant.EPOCH;

    public PagBankAgentRunner(
            AgentConfig config,
            DeviceTokenStore tokenStore,
            PendingResultStore pendingResultStore,
            BridgeClient bridgeClient,
            PaymentTerminalDriver driver,
            PaymentTerminalCapabilities capabilities,
            ObjectMapper mapper
    ) {
        this.config = config;
        this.tokenStore = tokenStore;
        this.pendingResultStore = pendingResultStore;
        this.bridgeClient = bridgeClient;
        this.driver = driver;
        this.capabilities = capabilities;
        this.mapper = mapper;
    }

    public void run() {
        Runtime.getRuntime().addShutdownHook(
                new Thread(this::close)
        );

        while (running.get()) {
            try {
                pairIfNeeded();
                heartbeatIfDue();
                pollAndProcessOnce();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                running.set(false);
            } catch (Exception exception) {
                LOGGER.log(
                        Level.WARNING,
                        "Falha temporaria no agente PagBank. "
                                + "Uma nova tentativa sera feita automaticamente.",
                        exception
                );

                waitBeforeRetry();
            }
        }
    }

    public DeviceToken pairIfNeeded()
            throws IOException, InterruptedException {
        if (currentToken != null) {
            return currentToken;
        }

        Optional<DeviceToken> stored =
                tokenStore.load();

        if (stored.isPresent()) {
            currentToken = stored.get();

            LOGGER.info(() ->
                    "Token local encontrado para device "
                            + currentToken.deviceId()
            );

            return currentToken;
        }

        if (config.pairingCode() == null
                || config.pairingCode().isBlank()) {

            throw new IllegalStateException(
                    "Informe V7_PAGBANK_AGENT_PAIRING_CODE "
                            + "para parear o agente."
            );
        }

        LOGGER.info(
                "Pareando agente PagBank local com o bridge."
        );

        BridgeDtos.PairResponse response =
                bridgeClient.pair(
                        new BridgeDtos.PairRequest(
                                config.pairingCode(),
                                "PAGBANK",
                                config.platform(),
                                config.agentVersion(),
                                config.externalTerminalReference(),
                                capabilities()
                        )
                );

        currentToken = new DeviceToken(
                response.deviceId(),
                response.deviceToken()
        );

        tokenStore.save(currentToken);

        LOGGER.info(() ->
                "Agente pareado. Device "
                        + currentToken.deviceId()
                        + "; token armazenado em "
                        + tokenStore.path()
        );

        return currentToken;
    }

    public void heartbeatIfDue()
            throws IOException, InterruptedException {
        if (Instant.now().isBefore(nextHeartbeatAt)) {
            return;
        }

        heartbeat();
    }

    public BridgeDtos.HeartbeatResponse heartbeat()
            throws IOException, InterruptedException {
        DeviceToken token = requireToken();

        BridgeDtos.HeartbeatResponse response =
                bridgeClient.heartbeat(
                        token,
                        new BridgeDtos.HeartbeatRequest(
                                "PAGBANK",
                                config.platform(),
                                config.agentVersion(),
                                config.externalTerminalReference(),
                                capabilities()
                        )
                );

        nextHeartbeatAt = Instant.now()
                .plus(config.heartbeatInterval());

        LOGGER.info(() ->
                "Heartbeat enviado. Device "
                        + response.deviceId()
                        + " status "
                        + response.status()
        );

        return response;
    }

    public Optional<BridgeDtos.ResultResponse>
    pollAndProcessOnce() throws Exception {
        Optional<BridgeDtos.ResultResponse> recovered =
                flushPendingResult();

        if (recovered.isPresent()) {
            return recovered;
        }

        DeviceToken token = requireToken();

        Optional<BridgeDtos.CommandDelivery> command =
                bridgeClient.nextCommand(
                        token,
                        config.longPollSeconds()
                );

        if (command.isEmpty()) {
            return Optional.empty();
        }

        BridgeDtos.CommandDelivery delivery =
                command.get();

        LOGGER.info(() ->
                "Comando recebido "
                        + delivery.commandId()
                        + " tipo "
                        + delivery.commandType()
        );

        bridgeClient.ack(
                token,
                delivery.commandId()
        );

        LOGGER.info(() ->
                "ACK enviado para comando "
                        + delivery.commandId()
        );

        Optional<BridgeDtos.ResultRequest> result =
                driver.processCommand(delivery);

        if (result.isEmpty()) {
            LOGGER.warning(() ->
                    "Modo TIMEOUT ativo para comando "
                            + delivery.commandId()
                            + "; resultado nao sera enviado."
            );

            return Optional.empty();
        }

        pendingResultStore.save(
                delivery.commandId(),
                result.get()
        );

        LOGGER.info(() ->
                "Resultado do comando "
                        + delivery.commandId()
                        + " salvo localmente antes do envio."
        );

        return flushPendingResult();
    }

    public Optional<BridgeDtos.ResultResponse>
    flushPendingResult()
            throws IOException, InterruptedException {
        Optional<PendingResultStore.PendingResult> pending =
                pendingResultStore.load();

        if (pending.isEmpty()) {
            return Optional.empty();
        }

        PendingResultStore.PendingResult result =
                pending.get();

        DeviceToken token = requireToken();

        LOGGER.info(() ->
                "Enviando resultado pendente do comando "
                        + result.commandId()
        );

        BridgeDtos.ResultResponse response =
                bridgeClient.result(
                        token,
                        result.commandId(),
                        result.result()
                );

        pendingResultStore.delete();

        LOGGER.info(() ->
                "Resultado confirmado pelo backend para comando "
                        + result.commandId()
                        + " status "
                        + response.commandStatus()
        );

        return Optional.of(response);
    }

    @Override
    public void close() {
        if (running.compareAndSet(true, false)) {
            if (driver instanceof AutoCloseable closeable) {
                try {
                    closeable.close();
                } catch (Exception exception) {
                    LOGGER.warning(() ->
                            "Falha ao liberar driver de terminal: "
                                    + exception.getMessage()
                    );
                }
            }

            LOGGER.info(
                    "Encerrando agente PagBank local com seguranca."
            );
        }
    }

    private DeviceToken requireToken() {
        if (currentToken == null) {
            currentToken = tokenStore.load()
                    .orElseThrow(() ->
                            new IllegalStateException(
                                    "Agente ainda nao pareado."
                            )
                    );
        }

        return currentToken;
    }

    private ObjectNode capabilities() {
        return capabilities.toJson(mapper);
    }

    private void waitBeforeRetry() {
        try {
            Thread.sleep(RETRY_DELAY_MILLIS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            running.set(false);
        }
    }
}
