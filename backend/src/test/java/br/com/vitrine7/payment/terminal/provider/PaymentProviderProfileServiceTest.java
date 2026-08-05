package br.com.vitrine7.payment.terminal.provider;

import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.payment.terminal.adapter.PaymentProviderAdapter;
import br.com.vitrine7.payment.terminal.adapter.PaymentTerminalAdapterRegistry;
import br.com.vitrine7.payment.terminal.dto.PaymentProviderAdminProfileResponse;
import br.com.vitrine7.payment.terminal.dto.PaymentProviderCatalogResponse;
import br.com.vitrine7.payment.terminal.dto.PaymentProviderProfileResponse;
import br.com.vitrine7.payment.terminal.dto.PaymentProviderPublicProfileResponse;
import br.com.vitrine7.payment.terminal.dto.PaymentProviderStatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentProviderProfileServiceTest {

    private final PaymentProviderCatalog catalog =
            new PaymentProviderCatalog();

    private final PaymentProviderProfileRepository profileRepository =
            mock(PaymentProviderProfileRepository.class);

    private final PaymentProviderConfigurationValidator validator =
            mock(PaymentProviderConfigurationValidator.class);

    private final PaymentProviderSecretsService secretsService =
            mock(PaymentProviderSecretsService.class);

    private PaymentProviderAdapter simulatorAdapter;
    private PaymentProviderAdapter pagBankAdapter;
    private PaymentProviderProfileService service;

    @BeforeEach
    void setUp() {
        simulatorAdapter = mock(PaymentProviderAdapter.class);
        when(simulatorAdapter.providerCode())
                .thenReturn(PaymentProviderCode.SIMULATOR);

        pagBankAdapter = mock(PaymentProviderAdapter.class);
        when(pagBankAdapter.providerCode())
                .thenReturn(PaymentProviderCode.PAGBANK);

        PaymentTerminalAdapterRegistry adapterRegistry =
                new PaymentTerminalAdapterRegistry(
                        List.of(
                                simulatorAdapter,
                                pagBankAdapter
                        )
                );

        service = new PaymentProviderProfileService(
                catalog,
                profileRepository,
                validator,
                secretsService,
                adapterRegistry
        );
    }

    @Test
    void activateAllowsSimulatorWhenAdapterIsAvailable() {
        UUID id = UUID.randomUUID();

        PaymentProviderProfile inactiveProfile =
                profile(
                        id,
                        PaymentProviderCode.SIMULATOR,
                        true,
                        false
                );

        PaymentProviderProfile activeProfile =
                profile(
                        id,
                        PaymentProviderCode.SIMULATOR,
                        true,
                        true
                );

        when(profileRepository.findById(id))
                .thenReturn(Optional.of(inactiveProfile))
                .thenReturn(Optional.of(activeProfile));

        PaymentProviderProfileResponse response =
                service.activate(id, 10L);

        assertEquals("SIMULATOR", response.providerCode());
        assertEquals(true, response.active());

        verify(simulatorAdapter)
                .validateConfiguration(any());

        verify(profileRepository)
                .activate(id, 10L);
    }

    @Test
    void catalogMarksSimulatorAndPagbankAsActivable() {
        when(profileRepository.findAll())
                .thenReturn(List.of());

        List<PaymentProviderCatalogResponse> response =
                service.listCatalog();

        assertEquals(
                List.of(true, true),
                response.stream()
                        .map(
                                PaymentProviderCatalogResponse
                                        ::canBeActivated
                        )
                        .toList()
        );
    }

    @Test
    void getProviderProfileReturnsAvailablePagbankWithoutPersistedProfile() {
        when(
                profileRepository.findByProviderCode(
                        PaymentProviderCode.PAGBANK
                )
        ).thenReturn(Optional.empty());

        PaymentProviderAdminProfileResponse response =
                service.getProviderProfile("PAGBANK");

        assertEquals("PAGBANK", response.code());
        assertEquals(
                "AVAILABLE",
                response.implementationStatus()
        );
        assertEquals(true, response.adapterAvailable());
        assertEquals(true, response.canBeActivated());
        assertEquals(false, response.configured());
        assertEquals(null, response.profile());
    }

    @Test
    void activateProviderReturnsActiveSimulatorProfileWhenAlreadyActive() {
        UUID id = UUID.randomUUID();

        PaymentProviderProfile activeProfile =
                profile(
                        id,
                        PaymentProviderCode.SIMULATOR,
                        true,
                        true
                );

        when(
                profileRepository.findByProviderCode(
                        PaymentProviderCode.SIMULATOR
                )
        ).thenReturn(Optional.of(activeProfile));

        PaymentProviderPublicProfileResponse response =
                service.activateProvider(
                        "SIMULATOR",
                        10L
                );

        assertEquals("SIMULATOR", response.providerCode());
        assertEquals(true, response.active());

        verify(profileRepository, never())
                .activate(any(), any());
    }

    @Test
    void activateProviderActivatesInactiveSimulatorProfile() {
        UUID id = UUID.randomUUID();

        PaymentProviderProfile inactiveProfile =
                profile(
                        id,
                        PaymentProviderCode.SIMULATOR,
                        true,
                        false
                );

        PaymentProviderProfile activeProfile =
                profile(
                        id,
                        PaymentProviderCode.SIMULATOR,
                        true,
                        true
                );

        when(
                profileRepository.findByProviderCode(
                        PaymentProviderCode.SIMULATOR
                )
        ).thenReturn(Optional.of(inactiveProfile));

        when(profileRepository.findById(id))
                .thenReturn(Optional.of(inactiveProfile))
                .thenReturn(Optional.of(activeProfile))
                .thenReturn(Optional.of(activeProfile));

        PaymentProviderPublicProfileResponse response =
                service.activateProvider(
                        "SIMULATOR",
                        10L
                );

        assertEquals("SIMULATOR", response.providerCode());
        assertEquals(true, response.active());

        verify(simulatorAdapter)
                .validateConfiguration(any());

        verify(profileRepository)
                .activate(id, 10L);
    }

    @Test
    void activateProviderActivatesInactivePagbankProfile() {
        UUID id = UUID.randomUUID();

        PaymentProviderProfile inactiveProfile =
                profile(
                        id,
                        PaymentProviderCode.PAGBANK,
                        true,
                        false
                );

        PaymentProviderProfile activeProfile =
                profile(
                        id,
                        PaymentProviderCode.PAGBANK,
                        true,
                        true
                );

        when(
                profileRepository.findByProviderCode(
                        PaymentProviderCode.PAGBANK
                )
        ).thenReturn(Optional.of(inactiveProfile));

        when(profileRepository.findById(id))
                .thenReturn(Optional.of(inactiveProfile))
                .thenReturn(Optional.of(activeProfile))
                .thenReturn(Optional.of(activeProfile));

        PaymentProviderPublicProfileResponse response =
                service.activateProvider(
                        "PAGBANK",
                        10L
                );

        assertEquals("PAGBANK", response.providerCode());
        assertEquals(true, response.active());

        verify(pagBankAdapter)
                .validateConfiguration(any());

        verify(profileRepository)
                .activate(id, 10L);
    }

    @Test
    void statusReturnsPublicActiveProviderDetails() {
        UUID id = UUID.randomUUID();

        PaymentProviderProfile activeProfile =
                profile(
                        id,
                        PaymentProviderCode.SIMULATOR,
                        true,
                        true
                );

        when(profileRepository.findActive())
                .thenReturn(Optional.of(activeProfile));

        PaymentProviderStatusResponse response =
                service.status();

        assertEquals(id, response.activeProfileId());
        assertEquals(
                "SIMULATOR",
                response.providerCode()
        );
        assertEquals(
                "Terminal Simulado",
                response.displayName()
        );
        assertEquals(
                "AVAILABLE",
                response.implementationStatus()
        );
        assertEquals(true, response.adapterAvailable());
        assertEquals(
                "SIMULATOR",
                response.profile().providerCode()
        );
    }

    @Test
    void activateProviderRejectsPagbankWhenAdapterIsMissing() {
        PaymentTerminalAdapterRegistry adapterRegistry =
                new PaymentTerminalAdapterRegistry(
                        List.of(simulatorAdapter)
                );

        PaymentProviderProfileService serviceWithoutPagBank =
                new PaymentProviderProfileService(
                        catalog,
                        profileRepository,
                        validator,
                        secretsService,
                        adapterRegistry
                );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> serviceWithoutPagBank.activateProvider(
                        "PAGBANK",
                        10L
                )
        );

        assertEquals(
                "PAYMENT_PROVIDER_ADAPTER_NOT_AVAILABLE",
                exception.getCode()
        );

        verify(profileRepository, never())
                .activate(any(), any());
    }

    @Test
    void activateRejectsPagbankWhenAdapterIsMissing() {
        UUID id = UUID.randomUUID();

        PaymentTerminalAdapterRegistry adapterRegistry =
                new PaymentTerminalAdapterRegistry(
                        List.of(simulatorAdapter)
                );

        PaymentProviderProfileService serviceWithoutPagBank =
                new PaymentProviderProfileService(
                        catalog,
                        profileRepository,
                        validator,
                        secretsService,
                        adapterRegistry
                );

        when(profileRepository.findById(id))
                .thenReturn(Optional.of(
                        profile(
                                id,
                                PaymentProviderCode.PAGBANK,
                                true,
                                false
                        )
                ));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> serviceWithoutPagBank.activate(
                        id,
                        10L
                )
        );

        assertEquals(
                "PAYMENT_PROVIDER_ADAPTER_NOT_AVAILABLE",
                exception.getCode()
        );

        verify(profileRepository, never())
                .activate(any(), any());
    }

    private PaymentProviderProfile profile(
            UUID id,
            PaymentProviderCode providerCode,
            boolean enabled,
            boolean active
    ) {
        OffsetDateTime now = OffsetDateTime.now();

        return new PaymentProviderProfile(
                id,
                providerCode,
                providerCode.name(),
                PaymentProviderEnvironment.LOCAL,
                enabled,
                active,
                null,
                null,
                null,
                false,
                List.of(),
                1L,
                now,
                now,
                10L,
                10L,
                1L
        );
    }
}
