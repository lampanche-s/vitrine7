package br.com.vitrine7.pagbankagent;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.UUID;

public class BridgeClient {

    private static final String TOKEN_HEADER = "X-Terminal-Device-Token";

    private final AgentConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public BridgeClient(
            AgentConfig config,
            HttpClient httpClient,
            ObjectMapper mapper
    ) {
        this.config = config;
        this.httpClient = httpClient;
        this.mapper = mapper;
    }

    public BridgeDtos.PairResponse pair(
            BridgeDtos.PairRequest request
    ) throws IOException, InterruptedException {
        return send(
                basePost("/pair", request),
                BridgeDtos.PairResponse.class
        );
    }

    public BridgeDtos.HeartbeatResponse heartbeat(
            DeviceToken token,
            BridgeDtos.HeartbeatRequest request
    ) throws IOException, InterruptedException {
        return send(
                authenticatedPost("/heartbeat", token, request),
                BridgeDtos.HeartbeatResponse.class
        );
    }

    public Optional<BridgeDtos.CommandDelivery> nextCommand(
            DeviceToken token,
            int waitSeconds
    ) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(
                        config.bridgeUri("/commands/next?waitSeconds=" + waitSeconds)
                )
                .header(TOKEN_HEADER, token.token())
                .GET()
                .build();
        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 204) {
            return Optional.empty();
        }
        ensureSuccess(response);
        return Optional.of(mapper.readValue(
                response.body(),
                BridgeDtos.CommandDelivery.class
        ));
    }

    public BridgeDtos.Acknowledgement ack(
            DeviceToken token,
            UUID commandId
    ) throws IOException, InterruptedException {
        return send(
                authenticatedPost(
                        "/commands/" + commandId + "/ack",
                        token,
                        null
                ),
                BridgeDtos.Acknowledgement.class
        );
    }

    public BridgeDtos.ResultResponse result(
            DeviceToken token,
            UUID commandId,
            BridgeDtos.ResultRequest request
    ) throws IOException, InterruptedException {
        return send(
                authenticatedPost(
                        "/commands/" + commandId + "/result",
                        token,
                        request
                ),
                BridgeDtos.ResultResponse.class
        );
    }

    private HttpRequest basePost(
            String path,
            Object body
    ) throws IOException {
        return HttpRequest.newBuilder(config.bridgeUri(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        body == null ? "{}" : mapper.writeValueAsString(body)
                ))
                .build();
    }

    private HttpRequest authenticatedPost(
            String path,
            DeviceToken token,
            Object body
    ) throws IOException {
        return HttpRequest.newBuilder(config.bridgeUri(path))
                .header("Content-Type", "application/json")
                .header(TOKEN_HEADER, token.token())
                .POST(HttpRequest.BodyPublishers.ofString(
                        body == null ? "{}" : mapper.writeValueAsString(body)
                ))
                .build();
    }

    private <T> T send(
            HttpRequest request,
            Class<T> responseType
    ) throws IOException, InterruptedException {
        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        ensureSuccess(response);
        return mapper.readValue(response.body(), responseType);
    }

    private void ensureSuccess(
            HttpResponse<String> response
    ) throws IOException {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException(
                    "Bridge HTTP " + response.statusCode()
                            + ": " + response.body()
            );
        }
    }
}
