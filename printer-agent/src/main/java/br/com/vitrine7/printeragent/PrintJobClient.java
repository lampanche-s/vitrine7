package br.com.vitrine7.printeragent;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public final class PrintJobClient {
    private static final String TOKEN_HEADER = "X-Printer-Agent-Token";
    private static final String JOB_ID_HEADER = "X-Print-Job-Id";
    private static final String ATTEMPT_HEADER = "X-Print-Attempt";

    private final AgentConfig config;
    private final HttpClient httpClient;

    public PrintJobClient(AgentConfig config) {
        this(
                config,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build()
        );
    }

    PrintJobClient(AgentConfig config, HttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient;
    }

    public Optional<Delivery> next() throws IOException, InterruptedException {
        URI uri = URI.create(
                config.backendUrl()
                        + "/api/v1/printer-agent/jobs/next?waitSeconds="
                        + config.waitSeconds()
        );

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(config.requestTimeout())
                .header("Accept", "text/plain")
                .header(TOKEN_HEADER, config.agentToken())
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        if (response.statusCode() == 204) {
            return Optional.empty();
        }

        ensureSuccess(response, "buscar impressao");

        String jobId = response.headers()
                .firstValue(JOB_ID_HEADER)
                .orElseThrow(() -> new IOException("Servidor nao informou o ID da impressao."));
        String attempt = response.headers()
                .firstValue(ATTEMPT_HEADER)
                .orElse("1");

        try {
            return Optional.of(new Delivery(
                    UUID.fromString(jobId),
                    response.body(),
                    Integer.parseInt(attempt)
            ));
        } catch (RuntimeException exception) {
            throw new IOException("Resposta de impressao invalida.", exception);
        }
    }

    public void report(UUID jobId, boolean success, String errorMessage)
            throws IOException, InterruptedException {
        String body = success || errorMessage == null ? "" : errorMessage;
        String successValue = URLEncoder.encode(
                Boolean.toString(success),
                StandardCharsets.UTF_8
        );

        HttpRequest request = HttpRequest.newBuilder(
                        URI.create(
                                config.backendUrl()
                                        + "/api/v1/printer-agent/jobs/"
                                        + jobId
                                        + "/result?success="
                                        + successValue
                        )
                )
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .header("Content-Type", "text/plain; charset=UTF-8")
                .header(TOKEN_HEADER, config.agentToken())
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        ensureSuccess(response, "confirmar impressao");
    }

    private void ensureSuccess(HttpResponse<String> response, String operation) throws IOException {
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return;
        }

        String body = response.body() == null ? "" : response.body().trim();
        throw new IOException(
                "Falha ao " + operation + ": HTTP " + response.statusCode()
                        + (body.isEmpty() ? "" : " - " + body)
        );
    }

    public record Delivery(UUID id, String receiptText, int attempt) {
    }
}
