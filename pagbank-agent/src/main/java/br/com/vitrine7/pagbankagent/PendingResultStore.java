package br.com.vitrine7.pagbankagent;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

public final class PendingResultStore {

    private final Path path;
    private final ObjectMapper mapper;

    public PendingResultStore(
            Path path,
            ObjectMapper mapper
    ) throws IOException {
        this.path = path;
        this.mapper = mapper;

        Path parent = path.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    public Optional<PendingResult> load() {
        if (!Files.exists(path)) {
            return Optional.empty();
        }

        try {
            return Optional.of(
                    mapper.readValue(
                            path.toFile(),
                            PendingResult.class
                    )
            );
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException(
                    "O resultado pendente nao pode ser lido em "
                            + path
                            + ". O agente nao recebera novas cobrancas "
                            + "ate esse arquivo ser recuperado.",
                    exception
            );
        }
    }

    public void save(
            UUID commandId,
            BridgeDtos.ResultRequest result
    ) throws IOException {
        PendingResult pendingResult =
                new PendingResult(
                        commandId,
                        result
                );

        Path temporaryPath = path.resolveSibling(
                path.getFileName() + ".tmp"
        );

        mapper.writeValue(
                temporaryPath.toFile(),
                pendingResult
        );

        try {
            Files.move(
                    temporaryPath,
                    path,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(
                    temporaryPath,
                    path,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
    }

    public void delete() throws IOException {
        Files.deleteIfExists(path);
    }

    public Path path() {
        return path;
    }

    public record PendingResult(
            UUID commandId,
            BridgeDtos.ResultRequest result
    ) {
    }
}
