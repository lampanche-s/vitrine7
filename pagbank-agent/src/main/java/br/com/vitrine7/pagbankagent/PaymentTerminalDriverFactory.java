package br.com.vitrine7.pagbankagent;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class PaymentTerminalDriverFactory {

    private PaymentTerminalDriverFactory() {
    }

    public static PaymentTerminalDriverSelection create(
            AgentConfig config,
            ObjectMapper mapper
    ) {
        return create(config, mapper, false);
    }

    static PaymentTerminalDriverSelection create(
            AgentConfig config,
            ObjectMapper mapper,
            boolean skipNativeLoad
    ) {
        return switch (config.driverMode()) {
            case SIMULATED -> new PaymentTerminalDriverSelection(
                    new SimulatedPaymentTerminalDriver(
                            config.developmentOutcome(),
                            mapper
                    ),
                    new PaymentTerminalCapabilities(
                            true,
                            true,
                            false,
                            true,
                            "SIMULATED"
                    )
            );
            case PLUGPAG -> new PaymentTerminalDriverSelection(
                    new PlugPagPaymentTerminalDriver(
                            config.plugPagJarPath(),
                            config.plugPagNativePath(),
                            config.plugPagBluetoothAddress(),
                            config.plugPagAppName(),
                            config.plugPagAppVersion(),
                            mapper,
                            skipNativeLoad
                    ),
                    new PaymentTerminalCapabilities(
                            true,
                            true,
                            false,
                            false,
                            "PLUGPAG"
                    )
            );
        };
    }
}
