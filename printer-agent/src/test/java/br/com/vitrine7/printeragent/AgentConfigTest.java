package br.com.vitrine7.printeragent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AgentConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsPropertiesAndAllowsEnvironmentOverride() throws Exception {
        Path config = tempDir.resolve("printer-agent.properties");
        Files.writeString(config, """
                backend.url=https://example.test/
                agent.token=file-token
                printer.name=Thermal 80
                poll.wait.seconds=18
                """);

        AgentConfig loaded = AgentConfig.load(
                config,
                Map.of("V7_PRINTER_AGENT_TOKEN", "env-token")
        );

        assertEquals("https://example.test", loaded.backendUrl());
        assertEquals("env-token", loaded.agentToken());
        assertEquals("Thermal 80", loaded.printerName());
        assertEquals(18, loaded.waitSeconds());
        assertEquals(80d, loaded.paperWidthMm());
    }
}
