package br.com.vitrine7.payment.terminal.adapter;

import br.com.vitrine7.payment.terminal.entity.PaymentTerminalMode;

public interface PaymentTerminalAdapter {

    boolean supports(PaymentTerminalMode mode);

    TerminalAdapterResult process(
            TerminalAdapterCommand command
    );
}
