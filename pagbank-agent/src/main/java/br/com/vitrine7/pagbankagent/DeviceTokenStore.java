package br.com.vitrine7.pagbankagent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

public class DeviceTokenStore {

    private final Path path;

    public DeviceTokenStore(Path path) {
        this.path = path;
    }

    public Optional<DeviceToken> load() {
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
            String deviceId = properties.getProperty("deviceId");
            String token = properties.getProperty("deviceToken");
            if (deviceId == null || token == null || token.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new DeviceToken(UUID.fromString(deviceId), token));
        } catch (IOException | RuntimeException exception) {
            return Optional.empty();
        }
    }

    public void save(DeviceToken token) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Properties properties = new Properties();
        properties.setProperty("deviceId", token.deviceId().toString());
        properties.setProperty("deviceToken", token.token());
        try (OutputStream output = Files.newOutputStream(path)) {
            properties.store(output, "Vitrine 7 PagBank local agent device token");
        }
    }

    public Path path() {
        return path;
    }
}
