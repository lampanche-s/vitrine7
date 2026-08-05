package br.com.vitrine7.pagbankagent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.tools.ToolProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PagBankAgentRunnerTest {

    private final ObjectMapper mapper =
            PagBankAgentApplication.objectMapper();

    private FakeBridge bridge;

    @TempDir
    Path tempDir;

    @AfterEach
    void tearDown() {
        if (bridge != null) {
            bridge.close();
        }

        System.clearProperty(
                "v7.fakePlugPag.reversalCalls"
        );

        System.clearProperty(
                "v7.fakePlugPag.reversalResult"
        );

        System.clearProperty(
                "v7.fakePlugPag.userReference"
        );
    }

    @Test
    void pairsStoresTokenAndSendsHeartbeat() throws Exception {
        bridge = FakeBridge.start(mapper);
        Path tokenPath = tempDir.resolve("device-token.properties");
        PagBankAgentRunner runner = runner(
                bridge.baseUri(),
                tokenPath,
                "PAIR-123",
                PaymentOutcomeMode.APPROVED
        );

        DeviceToken token = runner.pairIfNeeded();
        runner.heartbeat();

        assertEquals(bridge.deviceId, token.deviceId());
        assertEquals("device-token-secret", token.token());
        assertTrue(Files.readString(tokenPath).contains("deviceToken=device-token-secret"));

        CapturedRequest pair = bridge.require("/api/v1/payment-terminal/bridge/pair");
        assertEquals("", pair.token());
        assertEquals("PAGBANK", pair.body().path("providerCode").asText());
        assertEquals("PAIR-123", pair.body().path("pairingCode").asText());

        CapturedRequest heartbeat = bridge.require("/api/v1/payment-terminal/bridge/heartbeat");
        assertEquals("device-token-secret", heartbeat.token());
        assertEquals("PAGBANK", heartbeat.body().path("providerCode").asText());
    }

    @Test
    void receivesCommandAcksAndReturnsApprovedResult() throws Exception {
        assertPaymentResult(PaymentOutcomeMode.APPROVED, "APPROVED", null);
    }

    @Test
    void receivesCommandAcksAndReturnsDeclinedResult() throws Exception {
        assertPaymentResult(PaymentOutcomeMode.DECLINED, "DECLINED", "51");
    }

    @Test
    void receivesCommandAcksAndReturnsErrorResult() throws Exception {
        assertPaymentResult(PaymentOutcomeMode.ERROR, "ERROR", "PAGBANK_AGENT_ERROR");
    }

    @Test
    void timeoutModeAcksAndDoesNotReturnResult() throws Exception {
        bridge = FakeBridge.start(mapper);
        Path tokenPath = tempDir.resolve("device-token.properties");
        new DeviceTokenStore(tokenPath).save(
                new DeviceToken(bridge.deviceId, "device-token-secret")
        );
        PagBankAgentRunner runner = runner(
                bridge.baseUri(),
                tokenPath,
                null,
                PaymentOutcomeMode.TIMEOUT
        );
        runner.pairIfNeeded();

        assertTrue(runner.pollAndProcessOnce().isEmpty());

        bridge.require("/api/v1/payment-terminal/bridge/commands/"
                + bridge.commandId
                + "/ack");
        assertFalse(bridge.contains("/api/v1/payment-terminal/bridge/commands/"
                + bridge.commandId
                + "/result"));
    }

    @Test
    void selectsSimulatedDriverByConfiguration() {
        AgentConfig config = config(
                URI.create("http://127.0.0.1"),
                tempDir.resolve("sim.properties"),
                null,
                PaymentTerminalDriverMode.SIMULATED,
                PaymentOutcomeMode.APPROVED,
                null,
                null,
                null
        );

        PaymentTerminalDriverSelection selection =
                PaymentTerminalDriverFactory.create(config, mapper);

        assertInstanceOf(SimulatedPaymentTerminalDriver.class, selection.driver());
        assertTrue(selection.capabilities().simulated());
        assertTrue(selection.capabilities().credit());
        assertTrue(selection.capabilities().debit());
        assertFalse(selection.capabilities().pix());
    }

    @Test
    void selectsPlugPagDriverByConfigurationWithoutExposingPix() throws Exception {
        FakePlugPagSdk sdk = FakePlugPagSdk.create(tempDir);
        AgentConfig config = config(
                URI.create("http://127.0.0.1"),
                tempDir.resolve("plugpag.properties"),
                null,
                PaymentTerminalDriverMode.PLUGPAG,
                PaymentOutcomeMode.APPROVED,
                sdk.jarPath(),
                sdk.nativePath(),
                "00:11:22:33:44:55"
        );

        PaymentTerminalDriverSelection selection =
                PaymentTerminalDriverFactory.create(config, mapper, true);

        assertInstanceOf(PlugPagPaymentTerminalDriver.class, selection.driver());
        assertFalse(selection.capabilities().simulated());
        assertTrue(selection.capabilities().credit());
        assertTrue(selection.capabilities().debit());
        assertFalse(selection.capabilities().pix());
        ((PlugPagPaymentTerminalDriver) selection.driver()).close();
    }

    @Test
    void plugPagMapsCreditAndDebitMethods() throws Exception {
        FakePlugPagSdk sdk = FakePlugPagSdk.create(tempDir);
        try (PlugPagPaymentTerminalDriver driver = plugPagDriver(sdk)) {
            sdk.result(0, "Aprovada");
            driver.processPayment(command("CREDIT_CARD"));
            assertEquals("1", System.getProperty("v7.fakePlugPag.lastMethod"));
            assertEquals(
                    "SALE123ABC",
                    System.getProperty(
                            "v7.fakePlugPag.lastReference"
                    )
            );

            driver.processPayment(command("DEBIT_CARD"));
            assertEquals("2", System.getProperty("v7.fakePlugPag.lastMethod"));
        }
    }

    @Test
    void plugPagReturnsGenericErrorForUnsupportedMethods() throws Exception {
        FakePlugPagSdk sdk = FakePlugPagSdk.create(tempDir);
        try (PlugPagPaymentTerminalDriver driver = plugPagDriver(sdk)) {
            BridgeDtos.ResultRequest result =
                    driver.processPayment(command("UNSUPPORTED")).orElseThrow();

            assertEquals(ProviderPaymentStatus.ERROR, result.status());
            assertEquals("PAGBANK_AGENT_UNSUPPORTED_METHOD", result.failureCode());
        }
    }

    @Test
    void plugPagMapsApprovedDeclinedCancelledAndErrorResults() throws Exception {
        FakePlugPagSdk sdk = FakePlugPagSdk.create(tempDir);
        try (PlugPagPaymentTerminalDriver driver = plugPagDriver(sdk)) {
            sdk.result(0, "Aprovada");
            BridgeDtos.ResultRequest approved =
                    driver.processPayment(command("CREDIT_CARD")).orElseThrow();
            assertEquals(ProviderPaymentStatus.APPROVED, approved.status());
            assertEquals("TX123", approved.providerReference());
            assertEquals("NSU789", approved.authorizationCode());
            assertEquals(
                    "SALE123ABC",
                    approved.metadata()
                            .path("userReference")
                            .asText()
            );

            sdk.result(-1004, "Transacao negada pelo Host");
            BridgeDtos.ResultRequest declined =
                    driver.processPayment(command("CREDIT_CARD")).orElseThrow();
            assertEquals(ProviderPaymentStatus.DECLINED, declined.status());
            assertEquals("-1004", declined.failureCode());

            sdk.result(-1004, "Operacao cancelada pelo usuario");
            BridgeDtos.ResultRequest cancelled =
                    driver.processPayment(command("CREDIT_CARD")).orElseThrow();
            assertEquals(ProviderPaymentStatus.CANCELLED, cancelled.status());

            sdk.result(-1019, "Erro de comunicacao");
            BridgeDtos.ResultRequest error =
                    driver.processPayment(command("CREDIT_CARD")).orElseThrow();
            assertEquals(ProviderPaymentStatus.ERROR, error.status());
            assertEquals("-1019", error.failureCode());
        }
    }

    @Test
    void plugPagQueriesLastApprovedTransaction()
            throws Exception {
        FakePlugPagSdk sdk =
                FakePlugPagSdk.create(tempDir);

        System.setProperty(
                "v7.fakePlugPag.queryResult",
                "0"
        );

        System.setProperty(
                "v7.fakePlugPag.userReference",
                "SALE123"
        );

        try (PlugPagPaymentTerminalDriver driver =
                     plugPagDriver(sdk)) {

            BridgeDtos.ResultRequest result =
                    driver.processCommand(
                            queryCommand()
                    ).orElseThrow();

            assertEquals(
                    ProviderPaymentStatus.APPROVED,
                    result.status()
            );

            assertEquals(
                    "TX123",
                    result.providerReference()
            );

            assertEquals(
                    "NSU789",
                    result.authorizationCode()
            );

            assertEquals(
                    "SALE123",
                    result.metadata()
                            .path("userReference")
                            .asText()
            );

            assertEquals(
                    "2026-08-01",
                    result.metadata()
                            .path("transactionDate")
                            .asText()
            );

            assertEquals(
                    "PAXQ92TEST",
                    result.metadata()
                            .path("terminalSerialNumber")
                            .asText()
            );
        }
    }

    @Test
    void plugPagExecutesRealReversalCommand()
            throws Exception {
        FakePlugPagSdk sdk =
                FakePlugPagSdk.create(tempDir);

        System.setProperty(
                "v7.fakePlugPag.reversalResult",
                "0"
        );

        System.setProperty(
                "v7.fakePlugPag.userReference",
                "SALE123ABC"
        );

        try (PlugPagPaymentTerminalDriver driver =
                     plugPagDriver(sdk)) {

            BridgeDtos.ResultRequest result =
                    driver.processCommand(
                            reverseCommand()
                    ).orElseThrow();

            assertEquals(
                    ProviderPaymentStatus.APPROVED,
                    result.status()
            );

            assertEquals(
                    "TX123",
                    result.providerReference()
            );

            assertEquals(
                    "NSU789",
                    result.authorizationCode()
            );

            assertEquals(
                    "REVERSAL",
                    result.metadata()
                            .path("operationType")
                            .asText()
            );

            assertEquals(
                    "SALE123ABC",
                    result.metadata()
                            .path("userReference")
                            .asText()
            );

            assertEquals(
                    "1",
                    System.getProperty(
                            "v7.fakePlugPag.reversalCalls"
                    )
            );
        }
    }

    @Test
    void plugPagFailsClearlyWhenJarDllOrConfigurationIsMissing() throws Exception {
        FakePlugPagSdk sdk = FakePlugPagSdk.create(tempDir);

        IllegalStateException missingJar = assertThrows(
                IllegalStateException.class,
                () -> new PlugPagPaymentTerminalDriver(
                        tempDir.resolve("missing.jar"),
                        sdk.nativePath(),
                        "00:11:22:33:44:55",
                        "Vitrine 7",
                        "test",
                        mapper,
                        true
                )
        );
        assertTrue(missingJar.getMessage().contains("PlugPag.jar oficial"));

        Files.delete(sdk.nativePath().resolve("PlugPag.dll"));
        IllegalStateException missingDll = assertThrows(
                IllegalStateException.class,
                () -> new PlugPagPaymentTerminalDriver(
                        sdk.jarPath(),
                        sdk.nativePath(),
                        "00:11:22:33:44:55",
                        "Vitrine 7",
                        "test",
                        mapper,
                        true
                )
        );
        assertTrue(missingDll.getMessage().contains("DLL PlugPag ausente"));

        IllegalStateException invalidConfig = assertThrows(
                IllegalStateException.class,
                () -> new PlugPagPaymentTerminalDriver(
                        sdk.jarPath(),
                        sdk.nativePath(),
                        "",
                        "Vitrine 7",
                        "test",
                        mapper,
                        true
                )
        );
        assertTrue(invalidConfig.getMessage().contains("BT_ADDRESS"));
    }

    private void assertPaymentResult(
            PaymentOutcomeMode mode,
            String expectedStatus,
            String expectedFailureCode
    ) throws Exception {
        bridge = FakeBridge.start(mapper);
        Path tokenPath = tempDir.resolve(mode.name() + ".properties");
        new DeviceTokenStore(tokenPath).save(
                new DeviceToken(bridge.deviceId, "device-token-secret")
        );
        PagBankAgentRunner runner = runner(
                bridge.baseUri(),
                tokenPath,
                null,
                mode
        );
        runner.pairIfNeeded();

        assertTrue(runner.pollAndProcessOnce().isPresent());

        CapturedRequest next = bridge.require("/api/v1/payment-terminal/bridge/commands/next");
        assertEquals("device-token-secret", next.token());
        bridge.require("/api/v1/payment-terminal/bridge/commands/"
                + bridge.commandId
                + "/ack");
        CapturedRequest result = bridge.require("/api/v1/payment-terminal/bridge/commands/"
                + bridge.commandId
                + "/result");
        assertEquals(expectedStatus, result.body().path("status").asText());
        if (expectedFailureCode == null) {
            assertTrue(result.body().path("failureCode").isNull());
        } else {
            assertEquals(expectedFailureCode, result.body().path("failureCode").asText());
        }
    }

    private PagBankAgentRunner runner(
            URI baseUri,
            Path tokenPath,
            String pairingCode,
            PaymentOutcomeMode outcome
    ) throws IOException {
        AgentConfig config = new AgentConfig(
                baseUri,
                tokenPath,
                pairingCode,
                "LOCAL",
                "test-agent",
                "PAGBANK-TEST",
                PaymentTerminalDriverMode.SIMULATED,
                outcome,
                null,
                null,
                null,
                "Vitrine 7",
                "test",
                Duration.ofSeconds(60),
                0
        );
        return new PagBankAgentRunner(
                config,
                new DeviceTokenStore(tokenPath),
                new PendingResultStore(
                        tokenPath.resolveSibling(
                                "pending-result.json"
                        ),
                        mapper
                ),
                new BridgeClient(config, HttpClient.newHttpClient(), mapper),
                new SimulatedPaymentTerminalDriver(outcome, mapper),
                new PaymentTerminalCapabilities(true, true, false, true, "SIMULATED"),
                mapper
        );
    }

    private AgentConfig config(
            URI baseUri,
            Path tokenPath,
            String pairingCode,
            PaymentTerminalDriverMode driverMode,
            PaymentOutcomeMode outcome,
            Path jarPath,
            Path nativePath,
            String btAddress
    ) {
        return new AgentConfig(
                baseUri,
                tokenPath,
                pairingCode,
                "LOCAL",
                "test-agent",
                "PAGBANK-TEST",
                driverMode,
                outcome,
                jarPath,
                nativePath,
                btAddress,
                "Vitrine 7",
                "test",
                Duration.ofSeconds(60),
                0
        );
    }

    private PlugPagPaymentTerminalDriver plugPagDriver(
            FakePlugPagSdk sdk
    ) {
        return new PlugPagPaymentTerminalDriver(
                sdk.jarPath(),
                sdk.nativePath(),
                "00:11:22:33:44:55",
                "Vitrine 7",
                "test",
                mapper,
                true
        );
    }

    private BridgeDtos.CommandDelivery command(String paymentMethod) {
        return new BridgeDtos.CommandDelivery(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "INITIATE_PAYMENT",
                mapper.createObjectNode()
                        .put("amountCents", 1290)
                        .put(
                                "paymentMethod",
                                paymentMethod
                        )
                        .put(
                                "providerCode",
                                "PAGBANK"
                        )
                        .put(
                                "userReference",
                                "SALE123ABC"
                        ),
                OffsetDateTime.now().plusMinutes(1),
                1
        );
    }

    private BridgeDtos.CommandDelivery queryCommand() {
        return new BridgeDtos.CommandDelivery(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "QUERY_PAYMENT",
                mapper.createObjectNode()
                        .put(
                                "expectedUserReference",
                                "SALE123"
                        )
                        .put(
                                "providerCode",
                                "PAGBANK"
                        ),
                OffsetDateTime.now()
                        .plusMinutes(1),
                1
        );
    }

    private BridgeDtos.CommandDelivery reverseCommand() {
        return new BridgeDtos.CommandDelivery(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "REVERSE_PAYMENT",
                mapper.createObjectNode()
                        .put(
                                "expectedUserReference",
                                "SALE123ABC"
                        )
                        .put(
                                "providerCode",
                                "PAGBANK"
                        ),
                OffsetDateTime.now()
                        .plusMinutes(1),
                1
        );
    }

    private record FakePlugPagSdk(
            Path jarPath,
            Path nativePath
    ) {
        static FakePlugPagSdk create(Path tempDir) throws Exception {
            Path root = Files.createDirectories(
                    tempDir.resolve("fake-plugpag-" + UUID.randomUUID())
            );
            Path src = Files.createDirectories(
                    root.resolve("src/br/uol/pagseguro/client/plugpag")
            );
            Path classes = Files.createDirectories(root.resolve("classes"));
            Path source = src.resolve("PlugPag.java");
            Files.writeString(source, """
                    package br.uol.pagseguro.client.plugpag;

                    public class PlugPag {
                        public static final int RET_OK = 0;
                        public static final int TRANS_DENIED = -1004;
                        public static final int TRANS_NODATA = -1018;
                        public static final int CREDIT = 1;
                        public static final int DEBIT = 2;
                        public static final int A_VISTA = 1;
                        public PlugPag() {}
                        public int InitBTConnection(String value) {
                            System.setProperty("v7.fakePlugPag.bt", value);
                            return RET_OK;
                        }
                        public int SetVersionName(String name, String version) {
                            return RET_OK;
                        }
                        public int SimplePaymentTransaction(
                                int method,
                                int installmentType,
                                int installment,
                                String amount,
                                String reference
                        ) {
                            System.setProperty("v7.fakePlugPag.lastMethod", Integer.toString(method));
                            System.setProperty("v7.fakePlugPag.lastReference", reference);
                            return Integer.parseInt(System.getProperty("v7.fakePlugPag.result", "0"));
                        }
                        public int CancelTransaction() {
                            int calls = Integer.parseInt(
                                    System.getProperty(
                                            "v7.fakePlugPag.reversalCalls",
                                            "0"
                                    )
                            );

                            System.setProperty(
                                    "v7.fakePlugPag.reversalCalls",
                                    Integer.toString(calls + 1)
                            );

                            return Integer.parseInt(
                                    System.getProperty(
                                            "v7.fakePlugPag.reversalResult",
                                            "0"
                                    )
                            );
                        }
                        public int GetLastApprovedTransactionStatus() {
                            return Integer.parseInt(
                                    System.getProperty(
                                            "v7.fakePlugPag.queryResult",
                                            "0"
                                    )
                            );
                        }
                        public void UnloadDriverConnection() {}
                        public String getMessage() {
                            return System.getProperty("v7.fakePlugPag.message", "");
                        }
                        public String getTransactionCode() { return "TX123"; }
                        public String getHostNsu() { return "NSU789"; }
                        public String getCardBrand() { return "VISA"; }
                        public String getUserReference() {
                            return System.getProperty(
                                    "v7.fakePlugPag.userReference",
                                    System.getProperty(
                                            "v7.fakePlugPag.lastReference",
                                            ""
                                    )
                            );
                        }
                        public String getDate() {
                            return "2026-08-01";
                        }
                        public String getTime() {
                            return "12:34:56";
                        }
                        public String getTerminalSerialNumber() {
                            return "PAXQ92TEST";
                        }
                    }
                    """);
            int compileResult = ToolProvider.getSystemJavaCompiler().run(
                    null,
                    null,
                    null,
                    "-d",
                    classes.toString(),
                    source.toString()
            );
            assertEquals(0, compileResult);
            Path jar = root.resolve("PlugPag.jar");
            Process jarProcess = new ProcessBuilder(
                    "jar",
                    "cf",
                    jar.toString(),
                    "-C",
                    classes.toString(),
                    "."
            ).inheritIO().start();
            assertEquals(0, jarProcess.waitFor());

            Path nativePath = Files.createDirectories(root.resolve("native"));
            Files.writeString(nativePath.resolve("BTSerial.dll"), "");
            Files.writeString(nativePath.resolve("PPPagSeguro.dll"), "");
            Files.writeString(nativePath.resolve("PlugPag.dll"), "");
            return new FakePlugPagSdk(jar, nativePath);
        }

        void result(int code, String message) {
            System.setProperty("v7.fakePlugPag.result", Integer.toString(code));
            System.setProperty("v7.fakePlugPag.message", message);
        }
    }

    private record CapturedRequest(
            String method,
            String path,
            String token,
            JsonNode body
    ) {
    }

    private static final class FakeBridge implements AutoCloseable {
        private final ObjectMapper mapper;
        private final HttpServer server;
        private final UUID deviceId = UUID.randomUUID();
        private final UUID commandId = UUID.randomUUID();
        private final UUID transactionId = UUID.randomUUID();
        private final List<CapturedRequest> requests = new ArrayList<>();
        private boolean commandDelivered;

        private FakeBridge(
                ObjectMapper mapper,
                HttpServer server
        ) {
            this.mapper = mapper;
            this.server = server;
        }

        static FakeBridge start(
                ObjectMapper mapper
        ) throws IOException {
            HttpServer server = HttpServer.create(
                    new InetSocketAddress("127.0.0.1", 0),
                    0
            );
            FakeBridge bridge = new FakeBridge(mapper, server);
            server.createContext("/", bridge::handle);
            server.start();
            return bridge;
        }

        URI baseUri() {
            return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        }

        CapturedRequest require(
                String path
        ) {
            return requests.stream()
                    .filter(request -> request.path().equals(path))
                    .findFirst()
                    .orElseThrow();
        }

        boolean contains(
                String path
        ) {
            return requests.stream()
                    .anyMatch(request -> request.path().equals(path));
        }

        private void handle(
                HttpExchange exchange
        ) throws IOException {
            String path = exchange.getRequestURI().getPath();
            JsonNode body = readBody(exchange);
            requests.add(new CapturedRequest(
                    exchange.getRequestMethod(),
                    path,
                    exchange.getRequestHeaders()
                            .getFirst("X-Terminal-Device-Token") == null
                            ? ""
                            : exchange.getRequestHeaders()
                                    .getFirst("X-Terminal-Device-Token"),
                    body
            ));

            if (path.endsWith("/pair")) {
                write(exchange, 200, """
                        {
                          "deviceId": "%s",
                          "deviceToken": "device-token-secret",
                          "pairedAt": "%s"
                        }
                        """.formatted(deviceId, OffsetDateTime.now()));
                return;
            }

            if (path.endsWith("/heartbeat")) {
                write(exchange, 200, """
                        {
                          "deviceId": "%s",
                          "status": "ACTIVE",
                          "lastSeenAt": "%s"
                        }
                        """.formatted(deviceId, OffsetDateTime.now()));
                return;
            }

            if (path.endsWith("/commands/next")) {
                if (commandDelivered) {
                    write(exchange, 204, "");
                    return;
                }
                commandDelivered = true;
                write(exchange, 200, """
                        {
                          "commandId": "%s",
                          "transactionId": "%s",
                          "commandType": "INITIATE_PAYMENT",
                          "payload": {
                            "amountCents": 1290,
                            "paymentMethod": "CREDIT_CARD",
                            "providerCode": "PAGBANK"
                          },
                          "expiresAt": "%s",
                          "deliveryAttempt": 1
                        }
                        """.formatted(
                        commandId,
                        transactionId,
                        OffsetDateTime.now().plusMinutes(1)
                ));
                return;
            }

            if (path.endsWith("/ack")) {
                write(exchange, 200, """
                        {
                          "commandId": "%s",
                          "status": "ACKNOWLEDGED",
                          "acknowledgedAt": "%s"
                        }
                        """.formatted(commandId, OffsetDateTime.now()));
                return;
            }

            if (path.endsWith("/result")) {
                write(exchange, 200, """
                        {
                          "commandId": "%s",
                          "commandStatus": "COMPLETED",
                          "replayed": false
                        }
                        """.formatted(commandId));
                return;
            }

            write(exchange, 404, "{}");
        }

        private JsonNode readBody(
                HttpExchange exchange
        ) throws IOException {
            byte[] body = exchange.getRequestBody().readAllBytes();
            if (body.length == 0) {
                return mapper.createObjectNode();
            }
            return mapper.readTree(body);
        }

        private void write(
                HttpExchange exchange,
                int status,
                String body
        ) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
