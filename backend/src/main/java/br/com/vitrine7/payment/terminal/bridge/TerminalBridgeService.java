package br.com.vitrine7.payment.terminal.bridge;

import br.com.vitrine7.payment.terminal.adapter.ProviderPaymentCommand;
import br.com.vitrine7.payment.terminal.adapter.ProviderPaymentResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TerminalBridgeService {
    private final TerminalCommandQueueService commands;

    public ProviderPaymentResult initiatePayment(ProviderPaymentCommand command) {
        UUID commandId = commands.createInitiation(command.paymentId());
        return commands.waitForResult(commandId);
    }
}
