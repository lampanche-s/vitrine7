package br.com.vitrine7.backup.service;

import br.com.vitrine7.common.config.BusinessProperties;
import br.com.vitrine7.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class SystemBackupService {

    private static final DateTimeFormatter FILE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final DataSource dataSource;
    private final DataSourceProperties dataSourceProperties;
    private final Clock clock;
    private final ZoneId businessZone;
    private final String pgDumpPath;
    private final Duration timeout;
    private final ReentrantLock generationLock = new ReentrantLock();

    public SystemBackupService(
            DataSource dataSource,
            DataSourceProperties dataSourceProperties,
            Clock clock,
            BusinessProperties businessProperties,
            @Value("${app.backup.pg-dump-path:pg_dump}")
            String pgDumpPath,
            @Value("${app.backup.timeout:5m}")
            Duration timeout
    ) {
        this.dataSource = dataSource;
        this.dataSourceProperties = dataSourceProperties;
        this.clock = clock;
        this.businessZone = ZoneId.of(
                businessProperties.businessTimeZone()
        );
        this.pgDumpPath = pgDumpPath;
        this.timeout = timeout;
    }

    public GeneratedBackup generate() {
        if (!generationLock.tryLock()) {
            throw new BusinessException(
                    "SYSTEM_BACKUP_IN_PROGRESS",
                    "Já existe um backup em andamento. Aguarde a conclusão."
            );
        }

        Path backupFile = null;
        Path errorFile = null;

        try {
            PostgresConnection connection = resolveConnection();

            backupFile = Files.createTempFile(
                    "vitrine7-backup-",
                    ".backup"
            );

            errorFile = Files.createTempFile(
                    "vitrine7-pg-dump-",
                    ".log"
            );

            ProcessBuilder processBuilder = new ProcessBuilder(
                    pgDumpPath,
                    "--format=custom",
                    "--no-owner",
                    "--no-privileges",
                    "--file=" + backupFile.toAbsolutePath()
            );

            Map<String, String> environment =
                    processBuilder.environment();

            environment.put("PGHOST", connection.host());
            environment.put(
                    "PGPORT",
                    Integer.toString(connection.port())
            );
            environment.put("PGDATABASE", connection.database());
            environment.put("PGUSER", connection.username());
            environment.put("PGCONNECT_TIMEOUT", "10");

            if (
                    connection.password() != null
                            && !connection.password().isBlank()
            ) {
                environment.put(
                        "PGPASSWORD",
                        connection.password()
                );
            }

            if (
                    connection.sslMode() != null
                            && !connection.sslMode().isBlank()
            ) {
                environment.put(
                        "PGSSLMODE",
                        connection.sslMode()
                );
            }

            processBuilder.redirectOutput(
                    ProcessBuilder.Redirect.DISCARD
            );
            processBuilder.redirectError(errorFile.toFile());

            Process process = processBuilder.start();

            boolean completed = process.waitFor(
                    Math.max(timeout.toMillis(), 1L),
                    TimeUnit.MILLISECONDS
            );

            if (!completed) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);

                throw new BusinessException(
                        "SYSTEM_BACKUP_TIMEOUT",
                        "O backup excedeu o tempo limite permitido."
                );
            }

            if (
                    process.exitValue() != 0
                            || !Files.exists(backupFile)
                            || Files.size(backupFile) == 0L
            ) {
                String detail = readErrorDetail(errorFile);

                throw new BusinessException(
                        "SYSTEM_BACKUP_FAILED",
                        detail.isBlank()
                                ? "Não foi possível gerar o backup do sistema."
                                : "Não foi possível gerar o backup do sistema: "
                                + detail
                );
            }

            return new GeneratedBackup(
                    backupFile,
                    buildFileName(
                            clock.instant(),
                            businessZone
                    ),
                    Files.size(backupFile)
            );

        } catch (BusinessException exception) {
            deleteQuietly(backupFile);
            throw exception;

        } catch (IOException exception) {
            deleteQuietly(backupFile);

            throw new BusinessException(
                    "SYSTEM_BACKUP_UNAVAILABLE",
                    "O utilitário de backup do PostgreSQL não está disponível no servidor."
            );

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            deleteQuietly(backupFile);

            throw new BusinessException(
                    "SYSTEM_BACKUP_INTERRUPTED",
                    "A geração do backup foi interrompida."
            );

        } finally {
            deleteQuietly(errorFile);
            generationLock.unlock();
        }
    }

    public void delete(GeneratedBackup backup) {
        if (backup != null) {
            deleteQuietly(backup.path());
        }
    }

    private PostgresConnection resolveConnection() {
        try (Connection connection = dataSource.getConnection()) {
            String jdbcUrl = connection
                    .getMetaData()
                    .getURL();

            String username = firstNonBlank(
                    dataSourceProperties.getUsername(),
                    connection.getMetaData().getUserName()
            );

            return parseJdbcUrl(
                    jdbcUrl,
                    username,
                    dataSourceProperties.getPassword()
            );

        } catch (SQLException exception) {
            throw new BusinessException(
                    "SYSTEM_BACKUP_DATABASE_UNAVAILABLE",
                    "Não foi possível acessar o banco para gerar o backup."
            );
        }
    }

    static PostgresConnection parseJdbcUrl(
            String jdbcUrl,
            String username,
            String password
    ) {
        if (
                jdbcUrl == null
                        || !jdbcUrl.startsWith(
                        "jdbc:postgresql://"
                )
        ) {
            throw new BusinessException(
                    "SYSTEM_BACKUP_DATABASE_UNSUPPORTED",
                    "A configuração atual do banco não permite gerar o backup."
            );
        }

        URI uri;

        try {
            uri = URI.create(
                    jdbcUrl.substring("jdbc:".length())
            );
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(
                    "SYSTEM_BACKUP_DATABASE_UNSUPPORTED",
                    "A configuração atual do banco não permite gerar o backup."
            );
        }

        String host = uri.getHost();
        String path = uri.getPath();

        if (
                host == null
                        || host.isBlank()
                        || path == null
                        || path.length() <= 1
                        || username == null
                        || username.isBlank()
        ) {
            throw new BusinessException(
                    "SYSTEM_BACKUP_DATABASE_UNSUPPORTED",
                    "A configuração atual do banco não permite gerar o backup."
            );
        }

        String database = URLDecoder.decode(
                path.substring(1),
                StandardCharsets.UTF_8
        );

        String sslMode = queryParameter(
                uri.getRawQuery(),
                "sslmode"
        );

        return new PostgresConnection(
                host,
                uri.getPort() > 0
                        ? uri.getPort()
                        : 5432,
                database,
                username,
                password,
                sslMode
        );
    }

    static String buildFileName(
            Instant instant,
            ZoneId zoneId
    ) {
        return "vitrine7-backup-"
                + FILE_TIMESTAMP.format(
                instant.atZone(zoneId)
        )
                + ".backup";
    }

    private static String queryParameter(
            String rawQuery,
            String requestedName
    ) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return null;
        }

        for (String pair : rawQuery.split("&")) {
            String[] parts = pair.split("=", 2);
            String name = URLDecoder.decode(
                    parts[0],
                    StandardCharsets.UTF_8
            );

            if (!requestedName.equalsIgnoreCase(name)) {
                continue;
            }

            return parts.length == 2
                    ? URLDecoder.decode(
                    parts[1],
                    StandardCharsets.UTF_8
            )
                    : "";
        }

        return null;
    }

    private String readErrorDetail(Path errorFile) {
        if (errorFile == null || !Files.exists(errorFile)) {
            return "";
        }

        try {
            String detail = Files.readString(errorFile).trim();

            if (detail.length() > 500) {
                return detail.substring(0, 500);
            }

            return detail;
        } catch (IOException exception) {
            return "";
        }
    }

    private static String firstNonBlank(
            String preferred,
            String fallback
    ) {
        return preferred != null && !preferred.isBlank()
                ? preferred
                : fallback;
    }

    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }

        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // O arquivo temporário será removido pelo sistema operacional.
        }
    }

    public record GeneratedBackup(
            Path path,
            String fileName,
            long size
    ) {
    }

    record PostgresConnection(
            String host,
            int port,
            String database,
            String username,
            String password,
            String sslMode
    ) {
    }
}
