package br.com.vitrine7.pagbankagent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class PlugPagPaymentTerminalDriver
        implements PaymentTerminalDriver, AutoCloseable {

    private static final String PLUGPAG_CLASS =
            "br.uol.pagseguro.client.plugpag.PlugPag";
    private static final List<String> REQUIRED_DLLS = List.of(
            "BTSerial.dll",
            "PPPagSeguro.dll",
            "PlugPag.dll"
    );

    private final ObjectMapper mapper;
    private final String bluetoothAddress;
    private final String appName;
    private final String appVersion;
    private final URLClassLoader loader;
    private final Class<?> plugPagClass;
    private final int retOk;
    private final int transDenied;
    private final int transNoData;
    private final int credit;
    private final int debit;
    private final int cash;

    public PlugPagPaymentTerminalDriver(
            AgentConfig config,
            ObjectMapper mapper
    ) {
        this(
                config.plugPagJarPath(),
                config.plugPagNativePath(),
                config.plugPagBluetoothAddress(),
                config.plugPagAppName(),
                config.plugPagAppVersion(),
                mapper,
                false
        );
    }

    PlugPagPaymentTerminalDriver(
            Path jarPath,
            Path nativePath,
            String bluetoothAddress,
            String appName,
            String appVersion,
            ObjectMapper mapper
    ) {
        this(
                jarPath,
                nativePath,
                bluetoothAddress,
                appName,
                appVersion,
                mapper,
                false
        );
    }

    PlugPagPaymentTerminalDriver(
            Path jarPath,
            Path nativePath,
            String bluetoothAddress,
            String appName,
            String appVersion,
            ObjectMapper mapper,
            boolean skipNativeLoad
    ) {
        this.mapper = mapper;
        this.bluetoothAddress = requireText(
                bluetoothAddress,
                "Configure V7_PAGBANK_AGENT_PLUGPAG_BT_ADDRESS com a identificacao Bluetooth/MAC/porta da maquininha PlugPag."
        );
        this.appName = requireMax(
                appName,
                25,
                "Configure V7_PAGBANK_AGENT_PLUGPAG_APP_NAME com ate 25 caracteres."
        );
        this.appVersion = requireMax(
                appVersion,
                10,
                "Configure V7_PAGBANK_AGENT_PLUGPAG_APP_VERSION com ate 10 caracteres."
        );

        Path requiredJar = requireFile(
                jarPath,
                "Configure V7_PAGBANK_AGENT_PLUGPAG_JAR apontando para o PlugPag.jar oficial."
        );
        Path requiredNativePath = requireDirectory(
                nativePath,
                "Configure V7_PAGBANK_AGENT_PLUGPAG_NATIVE_DIR apontando para a pasta com as DLLs oficiais do PlugPag."
        );
        validateNativeLibraries(requiredNativePath);

        try {
            this.loader = new NativeAwareUrlClassLoader(
                    new URL[]{requiredJar.toUri().toURL()},
                    PlugPagPaymentTerminalDriver.class.getClassLoader(),
                    skipNativeLoad ? null : requiredNativePath
            );
            this.plugPagClass = Class.forName(PLUGPAG_CLASS, true, loader);
            requireOfficialSurface();
            this.retOk = intConstant("RET_OK");
            this.transDenied = intConstant("TRANS_DENIED");
            this.transNoData = intConstant("TRANS_NODATA");
            this.credit = intConstant("CREDIT");
            this.debit = intConstant("DEBIT");
            this.cash = intConstant("A_VISTA");
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Nao foi possivel carregar o SDK PlugPag Java oficial: "
                            + rootMessage(exception),
                    exception
            );
        }
    }

    @Override
    public void close() throws Exception {
        loader.close();
    }

    @Override
    public Optional<BridgeDtos.ResultRequest> processPayment(
            BridgeDtos.CommandDelivery command
    ) {
        Object plugPag = null;
        String providerRequestId = command.commandId().toString();
        try {
            JsonNode payload = command.payload();
            long amountCents = payload.path("amountCents").asLong(-1);
            if (amountCents <= 0) {
                return Optional.of(error(
                        providerRequestId,
                        "PAGBANK_AGENT_INVALID_AMOUNT",
                        "Valor invalido para PlugPag. amountCents deve ser positivo.",
                        null
                ));
            }

            int method = paymentMethod(payload.path("paymentMethod").asText());
            String amount = Long.toString(amountCents);
            String reference = userReference(command, payload);

            plugPag = newPlugPag();

            BridgeDtos.ResultRequest setupFailure =
                    initializePlugPag(
                            plugPag,
                            providerRequestId
                    );

            if (setupFailure != null) {
                return Optional.of(setupFailure);
            }

            int result = (Integer) invoke(
                    plugPag,
                    "SimplePaymentTransaction",
                    int.class,
                    method,
                    int.class,
                    cash,
                    int.class,
                    1,
                    String.class,
                    amount,
                    String.class,
                    reference
            );
            return Optional.of(
                    mapResult(
                            plugPag,
                            result,
                            providerRequestId,
                            reference
                    )
            );
        } catch (UnsupportedPaymentMethodException exception) {
            return Optional.of(error(
                    providerRequestId,
                    "PAGBANK_AGENT_UNSUPPORTED_METHOD",
                    exception.getMessage(),
                    plugPag
            ));
        } catch (Exception exception) {
            return Optional.of(error(
                    providerRequestId,
                    "PAGBANK_AGENT_ERROR",
                    "Falha ao executar pagamento PlugPag: " + rootMessage(exception),
                    plugPag
            ));
        } finally {
            if (plugPag != null) {
                try {
                    invoke(plugPag, "UnloadDriverConnection");
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Override
    public Optional<BridgeDtos.ResultRequest>
    queryLastApprovedTransaction(
            BridgeDtos.CommandDelivery command
    ) {
        Object plugPag = null;

        String providerRequestId =
                command.commandId().toString();

        try {
            plugPag = newPlugPag();

            BridgeDtos.ResultRequest setupFailure =
                    initializePlugPag(
                            plugPag,
                            providerRequestId
                    );

            if (setupFailure != null) {
                return Optional.of(setupFailure);
            }

            int result = (Integer) invoke(
                    plugPag,
                    "GetLastApprovedTransactionStatus"
            );

            return Optional.of(
                    mapLastApprovedResult(
                            plugPag,
                            result,
                            providerRequestId
                    )
            );
        } catch (Exception exception) {
            return Optional.of(
                    error(
                            providerRequestId,
                            "PAGBANK_AGENT_QUERY_ERROR",
                            "Falha ao consultar a ultima transacao "
                                    + "aprovada no PlugPag: "
                                    + rootMessage(exception),
                            plugPag
                    )
            );
        } finally {
            if (plugPag != null) {
                try {
                    invoke(
                            plugPag,
                            "UnloadDriverConnection"
                    );
                } catch (Exception ignored) {
                }
            }
        }
    }

    private BridgeDtos.ResultRequest initializePlugPag(
            Object plugPag,
            String providerRequestId
    ) throws ReflectiveOperationException {
        int connectionResult = (Integer) invoke(
                plugPag,
                "InitBTConnection",
                String.class,
                bluetoothAddress
        );

        if (connectionResult != retOk) {
            return error(
                    providerRequestId,
                    "PAGBANK_AGENT_CONNECTION_FAILED",
                    sdkFailureMessage(
                            plugPag,
                            connectionResult,
                            "Nao foi possivel conectar ao terminal PlugPag."
                    ),
                    plugPag
            );
        }

        int versionResult = (Integer) invoke(
                plugPag,
                "SetVersionName",
                String.class,
                appName,
                String.class,
                appVersion
        );

        if (versionResult != retOk) {
            return error(
                    providerRequestId,
                    "PAGBANK_AGENT_VERSION_CONFIG_FAILED",
                    sdkFailureMessage(
                            plugPag,
                            versionResult,
                            "Nao foi possivel configurar a identificacao da aplicacao no PlugPag."
                    ),
                    plugPag
            );
        }

        return null;
    }

    private BridgeDtos.ResultRequest mapResult(
            Object plugPag,
            int result,
            String providerRequestId,
            String userReference
    ) throws ReflectiveOperationException {
        ObjectNode metadata = metadata(plugPag);
        metadata.put(
                "userReference",
                userReference
        );
        String message = safeSdkText(plugPag, "getMessage");
        String transactionCode = safeSdkText(plugPag, "getTransactionCode");
        String hostNsu = safeSdkText(plugPag, "getHostNsu");
        String sdkUserReference = safeSdkText(plugPag, "getUserReference");
        String providerReference = firstText(
                transactionCode,
                hostNsu,
                sdkUserReference
        );

        if (result == retOk) {
            return new BridgeDtos.ResultRequest(
                    ProviderPaymentStatus.APPROVED,
                    providerReference,
                    providerRequestId,
                    hostNsu,
                    null,
                    null,
                    metadata
            );
        }

        if (isCancelled(result, message)) {
            return new BridgeDtos.ResultRequest(
                    ProviderPaymentStatus.CANCELLED,
                    providerReference,
                    providerRequestId,
                    null,
                    Integer.toString(result),
                    failureMessage(result, message),
                    metadata
            );
        }

        if (result == transDenied) {
            return new BridgeDtos.ResultRequest(
                    ProviderPaymentStatus.DECLINED,
                    providerReference,
                    providerRequestId,
                    null,
                    Integer.toString(result),
                    failureMessage(result, message),
                    metadata
            );
        }

        return new BridgeDtos.ResultRequest(
                ProviderPaymentStatus.ERROR,
                providerReference,
                providerRequestId,
                null,
                Integer.toString(result),
                failureMessage(result, message),
                metadata
        );
    }

    private BridgeDtos.ResultRequest mapLastApprovedResult(
            Object plugPag,
            int result,
            String providerRequestId
    ) {
        ObjectNode metadata =
                lastApprovedMetadata(plugPag);

        String transactionCode =
                safeSdkText(
                        plugPag,
                        "getTransactionCode"
                );

        String hostNsu =
                safeSdkText(
                        plugPag,
                        "getHostNsu"
                );

        String userReference =
                safeSdkText(
                        plugPag,
                        "getUserReference"
                );

        String providerReference =
                firstText(
                        transactionCode,
                        hostNsu,
                        userReference
                );

        String message =
                safeSdkText(
                        plugPag,
                        "getMessage"
                );

        if (result == retOk) {
            if (providerReference == null) {
                return error(
                        providerRequestId,
                        "PAGBANK_LAST_APPROVED_INVALID",
                        "O PlugPag informou uma transacao "
                                + "aprovada sem identificadores.",
                        plugPag
                );
            }

            return new BridgeDtos.ResultRequest(
                    ProviderPaymentStatus.APPROVED,
                    providerReference,
                    providerRequestId,
                    hostNsu,
                    null,
                    null,
                    metadata
            );
        }

        if (result == transNoData) {
            return new BridgeDtos.ResultRequest(
                    ProviderPaymentStatus.UNKNOWN,
                    null,
                    providerRequestId,
                    null,
                    "PAGBANK_LAST_APPROVED_NOT_FOUND",
                    failureMessage(
                            result,
                            message == null
                                    ? "Nenhuma transacao aprovada foi encontrada."
                                    : message
                    ),
                    metadata
            );
        }

        return new BridgeDtos.ResultRequest(
                ProviderPaymentStatus.ERROR,
                providerReference,
                providerRequestId,
                null,
                Integer.toString(result),
                failureMessage(result, message),
                metadata
        );
    }

    private ObjectNode lastApprovedMetadata(
            Object plugPag
    ) {
        ObjectNode metadata = metadata(plugPag);

        metadata.put(
                "queryType",
                "LAST_APPROVED_TRANSACTION"
        );

        put(
                metadata,
                "userReference",
                safeSdkText(
                        plugPag,
                        "getUserReference"
                )
        );

        put(
                metadata,
                "transactionDate",
                safeSdkText(
                        plugPag,
                        "getDate"
                )
        );

        put(
                metadata,
                "transactionTime",
                safeSdkText(
                        plugPag,
                        "getTime"
                )
        );

        put(
                metadata,
                "terminalSerialNumber",
                safeSdkText(
                        plugPag,
                        "getTerminalSerialNumber"
                )
        );

        return metadata;
    }

    private int paymentMethod(String value) {
        String normalized = normalize(value);
        return switch (normalized) {
            case "CREDIT_CARD", "CREDIT", "CREDITO" -> credit;
            case "DEBIT_CARD", "DEBIT", "DEBITO" -> debit;
            default -> throw new UnsupportedPaymentMethodException(
                    "Metodo de pagamento nao suportado pelo PlugPag: " + value
            );
        };
    }

    private String userReference(
            BridgeDtos.CommandDelivery command,
            JsonNode payload
    ) {
        String configured =
                text(payload, "userReference");

        if (configured != null) {
            String normalized = configured
                    .trim()
                    .toUpperCase(Locale.ROOT);

            if (!normalized.matches(
                    "[A-Z0-9]{1,10}"
            )) {
                throw new IllegalArgumentException(
                        "userReference recebida do backend "
                                + "deve conter de 1 a 10 "
                                + "caracteres alfanumericos."
                );
            }

            return normalized;
        }

        /*
         * Compatibilidade com comandos criados antes
         * desta alteracao.
         */
        return command.transactionId()
                .toString()
                .replace("-", "")
                .substring(0, 10)
                .toUpperCase(Locale.ROOT);
    }

    private BridgeDtos.ResultRequest error(
            String providerRequestId,
            String failureCode,
            String failureMessage,
            Object plugPag
    ) {
        return new BridgeDtos.ResultRequest(
                ProviderPaymentStatus.ERROR,
                null,
                providerRequestId,
                null,
                failureCode,
                failureMessage,
                metadataQuietly(plugPag)
        );
    }

    private ObjectNode metadataQuietly(Object plugPag) {
        return metadata(plugPag);
    }

    private ObjectNode metadata(Object plugPag) {
        ObjectNode metadata = mapper.createObjectNode();
        metadata.put("terminalReference", bluetoothAddress);
        metadata.put("entryMode", "PLUGPAG_WINDOWS");
        if (plugPag != null) {
            put(metadata, "cardBrand", safeSdkText(plugPag, "getCardBrand"));
        }
        return metadata;
    }

    private String sdkFailureMessage(
            Object plugPag,
            int result,
            String fallback
    ) {
        String message = safeSdkText(plugPag, "getMessage");
        if (message == null || message.isBlank()) {
            return fallback + " Codigo PlugPag: " + result + ".";
        }
        return failureMessage(result, message);
    }

    private Object newPlugPag()
            throws ReflectiveOperationException {
        return plugPagClass.getConstructor().newInstance();
    }

    private Object invoke(
            Object target,
            String name,
            Object... signatureAndArguments
    ) throws ReflectiveOperationException {
        Class<?>[] signature = new Class<?>[signatureAndArguments.length / 2];
        Object[] arguments = new Object[signatureAndArguments.length / 2];
        for (int i = 0; i < signature.length; i++) {
            signature[i] = (Class<?>) signatureAndArguments[i * 2];
            arguments[i] = signatureAndArguments[i * 2 + 1];
        }
        Method method = plugPagClass.getMethod(name, signature);
        return method.invoke(target, arguments);
    }

    private Object invoke(
            Object target,
            String name
    ) throws ReflectiveOperationException {
        return plugPagClass.getMethod(name).invoke(target);
    }

    private void requireOfficialSurface()
            throws NoSuchMethodException {
        plugPagClass.getConstructor();
        plugPagClass.getMethod("InitBTConnection", String.class);
        plugPagClass.getMethod("SetVersionName", String.class, String.class);
        plugPagClass.getMethod("SimplePaymentTransaction",
                int.class, int.class, int.class, String.class, String.class);
        plugPagClass.getMethod(
                "GetLastApprovedTransactionStatus"
        );
        plugPagClass.getMethod("UnloadDriverConnection");
        plugPagClass.getMethod("getMessage");
        plugPagClass.getMethod("getTransactionCode");
        plugPagClass.getMethod("getHostNsu");
        plugPagClass.getMethod("getUserReference");
        plugPagClass.getMethod("getDate");
        plugPagClass.getMethod("getTime");
        plugPagClass.getMethod(
                "getTerminalSerialNumber"
        );
    }

    private int intConstant(String name)
            throws ReflectiveOperationException {
        return plugPagClass.getField(name).getInt(null);
    }

    private void validateNativeLibraries(Path nativePath) {
        for (String dll : REQUIRED_DLLS) {
            Path dllPath = nativePath.resolve(dll);
            if (!Files.isRegularFile(dllPath)) {
                throw new IllegalStateException(
                        "DLL PlugPag ausente: " + dllPath
                );
            }
        }
    }

    private static Path requireFile(Path path, String message) {
        if (path == null || !Files.isRegularFile(path)) {
            throw new IllegalStateException(message);
        }
        return path;
    }

    private static Path requireDirectory(Path path, String message) {
        if (path == null || !Files.isDirectory(path)) {
            throw new IllegalStateException(message);
        }
        return path;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
        return value.trim();
    }

    private static String requireMax(
            String value,
            int max,
            String message
    ) {
        String text = requireText(value, message);
        if (text.length() > max) {
            throw new IllegalStateException(message);
        }
        return text;
    }

    private static boolean isCancelled(int result, String message) {
        if (result == -202) {
            return true;
        }
        String normalized = normalize(message);
        return normalized.contains("CANCEL");
    }

    private static String failureMessage(int result, String message) {
        String text = message == null || message.isBlank()
                ? "PlugPag retornou codigo " + result + "."
                : message.trim();
        return text.length() <= 255 ? text : text.substring(0, 255);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            return null;
        }
        return value.asText().trim();
    }

    private static String safeSdkText(Object target, String getter) {
        if (target == null) {
            return null;
        }
        try {
            Object value = target.getClass().getMethod(getter).invoke(target);
            return value == null || value.toString().isBlank()
                    ? null
                    : value.toString().trim();
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }

    private static void put(ObjectNode node, String field, String value) {
        if (value != null && !value.isBlank()) {
            node.put(field, value);
        }
    }

    private static String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.trim().toUpperCase(Locale.ROOT);
    }

    private static String rootMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root instanceof InvocationTargetException
                && ((InvocationTargetException) root).getTargetException() != null) {
            root = ((InvocationTargetException) root).getTargetException();
        }
        return root.getMessage() == null
                ? root.getClass().getSimpleName()
                : root.getMessage();
    }

    private static final class UnsupportedPaymentMethodException
            extends RuntimeException {
        private UnsupportedPaymentMethodException(String message) {
            super(message);
        }
    }

    private static final class NativeAwareUrlClassLoader
            extends URLClassLoader {
        private final Path nativePath;

        private NativeAwareUrlClassLoader(
                URL[] urls,
                ClassLoader parent,
                Path nativePath
        ) {
            super(urls, parent);
            this.nativePath = nativePath;
        }

        @Override
        protected String findLibrary(String libname) {
            if (nativePath == null) {
                return super.findLibrary(libname);
            }
            String mappedName = System.mapLibraryName(libname);
            Path candidate = nativePath.resolve(mappedName);
            if (Files.isRegularFile(candidate)) {
                return candidate.toAbsolutePath().toString();
            }
            return super.findLibrary(libname);
        }
    }
}
