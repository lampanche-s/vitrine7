package br.com.vitrine7.catalog.service;

import br.com.vitrine7.catalog.exception.CatalogAccessDeniedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CatalogAccessServiceTest {

    @Test
    void acceptsOnlyTheConfiguredPassword() {
        CatalogAccessService service = new CatalogAccessService("segredo");

        assertThat(service.isPasswordValid("segredo")).isTrue();
        assertThat(service.isPasswordValid("incorreta")).isFalse();
        assertThat(service.isPasswordValid(null)).isFalse();
        assertThrows(CatalogAccessDeniedException.class, () -> service.requireAccess("incorreta"));
    }

    @Test
    void deniesWhenConfigurationIsMissing() {
        CatalogAccessService service = new CatalogAccessService("");
        assertThat(service.isPasswordValid("segredo")).isFalse();
        assertThrows(CatalogAccessDeniedException.class, () -> service.requireAccess("segredo"));
    }
}
