package br.com.vitrine7.payment.terminal.repository;

import br.com.vitrine7.payment.terminal.entity.PaymentTerminalSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTerminalSettingsRepository
        extends JpaRepository<PaymentTerminalSettingsEntity, Short> {
}
