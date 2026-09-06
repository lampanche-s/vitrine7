package br.com.vitrine7.printeragent;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrintJobClientTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void sendsAuthenticatedHeartbeatWithIdentityAndVersion() throws Exception {
        AtomicReference<String> path = new AtomicReference<>();
        AtomicReference<String> token = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/printer-agent/heartbeat", exchange -> {
            path.set(exchange.getRequestURI().toString());
            token.set(exchange.getRequestHeaders()
                    .getFirst("X-Printer-Agent-Token"));
            byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        PrintJobClient client = new PrintJobClient(config(server.getAddress().getPort()));
        client.heartbeat();

        assertEquals("test-token", token.get());
        assertTrue(path.get().contains("identity=caixa-principal"));
        assertTrue(path.get().contains("agentVersion=1.0.0"));
    }

    private AgentConfig config(int port) {
        return new AgentConfig(
                "http://127.0.0.1:" + port,
                "test-token",
                "caixa-principal",
                "1.0.0",
                30,
                "Thermal 80",
                0,
                80d,
                2d,
                7f,
                9f
        );
    }
}
