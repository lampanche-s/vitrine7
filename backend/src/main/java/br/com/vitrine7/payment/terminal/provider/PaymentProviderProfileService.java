package br.com.vitrine7.payment.terminal.provider;

import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.common.exception.NotFoundException;
import br.com.vitrine7.payment.terminal.adapter.PaymentProviderAdapter;
import br.com.vitrine7.payment.terminal.adapter.PaymentTerminalAdapterRegistry;
import br.com.vitrine7.payment.terminal.adapter.ProviderConfiguration;
import br.com.vitrine7.payment.terminal.dto.PaymentProviderAdminProfileResponse;
import br.com.vitrine7.payment.terminal.dto.PaymentProviderCatalogResponse;
import br.com.vitrine7.payment.terminal.dto.PaymentProviderProfileRequest;
import br.com.vitrine7.payment.terminal.dto.PaymentProviderProfileResponse;
import br.com.vitrine7.payment.terminal.dto.PaymentProviderPublicProfileResponse;
import br.com.vitrine7.payment.terminal.dto.PaymentProviderStatusResponse;
import br.com.vitrine7.payment.terminal.dto.PaymentProviderTestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentProviderProfileService {

    private final PaymentProviderCatalog catalog;
    private final PaymentProviderProfileRepository profileRepository;
    private final PaymentProviderConfigurationValidator validator;
    private final PaymentProviderSecretsService secretsService;
    private final PaymentTerminalAdapterRegistry adapterRegistry;

    @Transactional(readOnly = true)
    public List<PaymentProviderCatalogResponse> listCatalog() {
        List<PaymentProviderProfile> profiles =
                profileRepository.findAll();

        return catalog.list()
                .stream()
                .map(descriptor -> {
                    List<PaymentProviderProfile> providerProfiles =
                            profiles.stream()
                                    .filter(profile -> profile.providerCode()
                                            == descriptor.code())
                                    .toList();

                    return PaymentProviderCatalogResponse.from(
                            descriptor,
                            adapterRegistry.isAdapterAvailable(
                                    descriptor.code()
                            ),
                            canBeActivated(descriptor),
                            providerProfiles.stream()
                                    .anyMatch(PaymentProviderProfile::active),
                            !providerProfiles.isEmpty(),
                            providerProfiles.stream()
                                    .filter(PaymentProviderProfile::active)
                                    .findFirst()
                                    .or(() -> providerProfiles.stream()
                                            .findFirst())
                                    .map(profile -> profile.environment().name())
                                    .orElse(null)
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentProviderAdminProfileResponse getProviderProfile(
            String providerCode
    ) {
        PaymentProviderCode code = parseProviderCode(providerCode);
        validateAdministrativelySelectableProvider(code);

        PaymentProviderCatalog.ProviderDescriptor descriptor =
                catalog.require(code);
        PaymentProviderProfile profile =
                profileRepository.findByProviderCode(code)
                        .orElse(null);

        return PaymentProviderAdminProfileResponse.from(
                descriptor,
                adapterRegistry.isAdapterAvailable(code),
                canBeActivated(descriptor),
                profile
        );
    }

    @Transactional(readOnly = true)
    public List<PaymentProviderProfileResponse> listProfiles() {
        return profileRepository.findAll()
                .stream()
                .map(PaymentProviderProfileResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentProviderProfileResponse getProfile(UUID id) {
        return PaymentProviderProfileResponse.from(
                requireProfile(id)
        );
    }

    @Transactional
    public PaymentProviderProfileResponse create(
            PaymentProviderProfileRequest request,
            Long actorUserId
    ) {
        validateAdministrativelySelectableProvider(request.providerCode());
        Map<String, Object> publicConfiguration =
                validator.validatePublicConfiguration(
                        request.publicConfiguration()
                );
        Map<String, String> credentials =
                validator.validateCredentials(
                        request.credentials()
                );

        PaymentProviderSecretsService.EncryptedCredentials encrypted =
                secretsService.encrypt(credentials);

        if (Boolean.TRUE.equals(request.active())) {
            validateCanActivate(
                    request.providerCode(),
                    Boolean.TRUE.equals(request.enabled()),
                    request.environment(),
                    publicConfiguration,
                    encrypted != null
            );
        }

        UUID id = profileRepository.create(
                request.providerCode(),
                request.displayName().trim(),
                request.environment(),
                Boolean.TRUE.equals(request.enabled()),
                false,
                request.merchantReference(),
                request.terminalReference(),
                publicConfiguration,
                encrypted,
                credentials.keySet()
                        .stream()
                        .sorted()
                        .toList(),
                actorUserId
        );

        PaymentProviderProfile profile = requireProfile(id);

        if (Boolean.TRUE.equals(request.active())) {
            return activate(id, actorUserId);
        }

        return PaymentProviderProfileResponse.from(profile);
    }

    @Transactional
    public PaymentProviderProfileResponse update(
            UUID id,
            PaymentProviderProfileRequest request,
            Long actorUserId
    ) {
        PaymentProviderProfile current = requireProfile(id);
        if (request.providerCode() != current.providerCode()) {
            throw new BusinessException(
                    "PAYMENT_PROVIDER_CONFIGURATION_INVALID",
                    "Provider do perfil nao pode ser alterado."
            );
        }

        if (current.active()
                && Boolean.FALSE.equals(request.enabled())) {
            throw new BusinessException(
                    "PAYMENT_PROVIDER_PROFILE_INACTIVE",
                    "Perfil ativo nao pode ser desabilitado sem troca segura."
            );
        }

        Map<String, Object> publicConfiguration =
                validator.validatePublicConfiguration(
                        request.publicConfiguration()
                );
        Map<String, String> credentials =
                validator.validateCredentials(
                        request.credentials()
                );

        PaymentProviderSecretsService.EncryptedCredentials encrypted =
                secretsService.encrypt(credentials);

        profileRepository.update(
                id,
                request.displayName().trim(),
                request.environment(),
                Boolean.TRUE.equals(request.enabled()),
                request.merchantReference(),
                request.terminalReference(),
                publicConfiguration,
                encrypted,
                credentials.keySet()
                        .stream()
                        .sorted()
                        .toList(),
                request.version() == null
                        ? current.version()
                        : request.version(),
                actorUserId
        );

        PaymentProviderProfile updated = requireProfile(id);
        return PaymentProviderProfileResponse.from(updated);
    }

    @Transactional
    public PaymentProviderProfileResponse activate(
            UUID id,
            Long actorUserId
    ) {
        profileRepository.lockActiveProfiles();
        profileRepository.lockProfile(id);

        PaymentProviderProfile profile = requireProfile(id);
        if (profile.active()) {
            throw new BusinessException(
                    "PAYMENT_PROVIDER_ALREADY_ACTIVE",
                    "Perfil de provider ja esta ativo."
            );
        }

        validateCanActivate(
                profile.providerCode(),
                profile.enabled(),
                profile.environment(),
                Map.of(),
                profile.credentialsConfigured()
        );

        ProviderConfiguration configuration =
                new ProviderConfiguration(
                        profile.id(),
                        profile.providerCode(),
                        profile.environment(),
                        profile.configurationVersion(),
                        profile.publicConfiguration(),
                        profile.credentialsConfigured()
                );
        adapterRegistry.getAdapter(profile.providerCode())
                .validateConfiguration(configuration);

        profileRepository.activate(id, actorUserId);

        PaymentProviderProfile activated = requireProfile(id);
        return PaymentProviderProfileResponse.from(activated);
    }

    @Transactional
    public PaymentProviderPublicProfileResponse activateProvider(
            String providerCode,
            Long actorUserId
    ) {
        PaymentProviderCode code = parseProviderCode(providerCode);
        validateAdministrativelySelectableProvider(code);

        PaymentProviderCatalog.ProviderDescriptor descriptor =
                catalog.require(code);
        if (!canBeActivated(descriptor)) {
            validateCanActivate(
                    code,
                    true,
                    PaymentProviderEnvironment.LOCAL,
                    Map.of(),
                    false
            );
        }

        PaymentProviderProfile profile =
                profileRepository.findByProviderCode(code)
                        .orElseThrow(() -> new NotFoundException(
                                "PAYMENT_PROVIDER_PROFILE_NOT_FOUND",
                                "Perfil de provider nao encontrado."
                        ));

        if (profile.active()) {
            return PaymentProviderPublicProfileResponse.from(profile);
        }

        return PaymentProviderPublicProfileResponse.from(
                requireProfile(activate(profile.id(), actorUserId).id())
        );
    }

    @Transactional
    public PaymentProviderTestResponse test(
            UUID id,
            Long actorUserId
    ) {
        PaymentProviderProfile profile = requireProfile(id);
        PaymentProviderCatalog.ProviderDescriptor descriptor =
                catalog.require(profile.providerCode());

        boolean success = false;
        String code = descriptor.implementationStatus().name();
        String message = "Implementacao pendente.";

        if (profile.providerCode() == PaymentProviderCode.SIMULATOR) {
            PaymentProviderAdapter adapter =
                    adapterRegistry.getAdapter(profile.providerCode());
            adapter.validateConfiguration(
                    new ProviderConfiguration(
                            profile.id(),
                            profile.providerCode(),
                            profile.environment(),
                            profile.configurationVersion(),
                            profile.publicConfiguration(),
                            profile.credentialsConfigured()
                    )
            );
            success = true;
            code = "OK";
            message = "Configuracao local do simulador valida.";
        }


        return new PaymentProviderTestResponse(
                profile.providerCode().name(),
                descriptor.implementationStatus().name(),
                success,
                code,
                message
        );
    }

    @Transactional(readOnly = true)
    public PaymentProviderStatusResponse status() {
        PaymentProviderProfile profile =
                profileRepository.findActive()
                        .orElseThrow(() -> new NotFoundException(
                                "PAYMENT_PROVIDER_PROFILE_NOT_FOUND",
                                "Nenhum perfil de provider ativo foi encontrado."
                        ));
        PaymentProviderCatalog.ProviderDescriptor descriptor =
                catalog.require(profile.providerCode());
        return new PaymentProviderStatusResponse(
                profile.id(),
                profile.providerCode().name(),
                descriptor.displayName(),
                descriptor.implementationStatus().name(),
                profile.environment().name(),
                profile.enabled(),
                profile.configurationVersion(),
                adapterRegistry.isAdapterAvailable(profile.providerCode()),
                PaymentProviderPublicProfileResponse.from(profile)
        );
    }

    private void validateCanActivate(
            PaymentProviderCode providerCode,
            boolean enabled,
            PaymentProviderEnvironment environment,
            Map<String, Object> publicConfiguration,
            boolean credentialsConfigured
    ) {
        if (!enabled) {
            throw new BusinessException(
                    "PAYMENT_PROVIDER_PROFILE_DISABLED",
                    "Perfil desabilitado nao pode ser ativado."
            );
        }

        PaymentProviderCatalog.ProviderDescriptor descriptor =
                catalog.require(providerCode);

        if (descriptor.implementationStatus()
                == PaymentProviderImplementationStatus.IMPLEMENTATION_PENDING) {
            throw new BusinessException(
                    "PAYMENT_PROVIDER_IMPLEMENTATION_PENDING",
                    "Provider ainda nao possui implementacao disponivel."
            );
        }

        if (!adapterRegistry.isAdapterAvailable(providerCode)) {
            throw new BusinessException(
                    "PAYMENT_PROVIDER_ADAPTER_NOT_AVAILABLE",
                    "Adapter do provider nao esta disponivel."
            );
        }
    }

    private PaymentProviderProfile requireProfile(UUID id) {
        return profileRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "PAYMENT_PROVIDER_PROFILE_NOT_FOUND",
                        "Perfil de provider nao encontrado."
                ));
    }

    private void validateKnownProvider(PaymentProviderCode code) {
        if (code == null) {
            throw new BusinessException(
                    "PAYMENT_PROVIDER_NOT_FOUND",
                    "Provider de pagamento nao encontrado."
            );
        }
        catalog.require(code);
    }

    private void validateAdministrativelySelectableProvider(
            PaymentProviderCode code
    ) {
        validateKnownProvider(code);
        if (!catalog.isAdministrativelySelectable(code)) {
            throw new BusinessException(
                    "PAYMENT_PROVIDER_NOT_AVAILABLE",
                    "Provider de pagamento nao esta disponivel para configuracao administrativa."
            );
        }
    }

    private PaymentProviderCode parseProviderCode(String code) {
        try {
            return PaymentProviderCode.valueOf(code.toUpperCase());
        } catch (RuntimeException exception) {
            throw new NotFoundException(
                    "PAYMENT_PROVIDER_NOT_FOUND",
                    "Provider de pagamento nao encontrado."
            );
        }
    }

    private boolean canBeActivated(
            PaymentProviderCatalog.ProviderDescriptor descriptor
    ) {
        return descriptor.implementationStatus()
                == PaymentProviderImplementationStatus.AVAILABLE
                && adapterRegistry.isAdapterAvailable(descriptor.code());
    }
}
