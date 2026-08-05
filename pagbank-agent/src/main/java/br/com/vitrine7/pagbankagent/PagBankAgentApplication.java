package br.com.vitrine7.pagbankagent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.http.HttpClient;

public final class PagBankAgentApplication {

    private PagBankAgentApplication() {
    }

    public static void main(String[] args) throws Exception {
        AgentConfig config = AgentConfig.fromEnvironment();
        ObjectMapper mapper = objectMapper();
        BridgeClient bridgeClient = new BridgeClient(
                config,
                HttpClient.newHttpClient(),
                mapper
        );
        DeviceTokenStore tokenStore =
                new DeviceTokenStore(config.tokenStorePath());
        PendingResultStore pendingResultStore =
                new PendingResultStore(
                        config.tokenStorePath()
                                .resolveSibling(
                                        "pending-result.json"
                                ),
                        mapper
                );
        PaymentTerminalDriverSelection driverSelection =
                PaymentTerminalDriverFactory.create(config, mapper);

        try (PagBankAgentRunner runner = new PagBankAgentRunner(
                config,
                tokenStore,
                pendingResultStore,
                bridgeClient,
                driverSelection.driver(),
                driverSelection.capabilities(),
                mapper
        )) {
            runner.run();
        }
    }

    public static ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}
