package br.com.vitrine7.payment.terminal.bridge;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TerminalCommandExpirationService {

    private final TerminalCommandRepository commands;
    private final TerminalCommandQueueService queueService;
    private final Clock clock;

    @Transactional
    public int expireBatch() {
        List<TerminalCommandRepository.CommandSnapshot>
                expired =
                commands.expireDue(
                        OffsetDateTime.now(clock)
                );

        for (
                TerminalCommandRepository.CommandSnapshot command
                : expired
        ) {
            commands.mirrorDelivery(
                    command.id(),
                    "EXPIRED"
            );

            if (command.type().equals(
                    "INITIATE_PAYMENT"
            )) {
                queueService.createReconciliationQuery(
                        command.transactionId(),
                        command.deviceId()
                );
            }
        }

        return expired.size();
    }
}
