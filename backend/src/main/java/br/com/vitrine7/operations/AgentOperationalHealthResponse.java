package br.com.vitrine7.operations;

import java.time.OffsetDateTime;
import java.util.List;

public record AgentOperationalHealthResponse(
        OffsetDateTime checkedAt,
        List<Agent> agents
) {
    public record Agent(
            String agent,
            AgentOperationalState status,
            String identity,
            String version,
            OffsetDateTime lastSeenAt
    ) {
    }
}
