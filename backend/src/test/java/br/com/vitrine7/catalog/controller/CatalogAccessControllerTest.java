package br.com.vitrine7.catalog.controller;

import br.com.vitrine7.catalog.service.CatalogAccessService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CatalogAccessControllerTest {
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new CatalogAccessController(new CatalogAccessService("segredo")))
            .build();

    @Test
    void verifiesPassword() throws Exception {
        mockMvc.perform(post("/api/v1/catalog/access/verify").contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"segredo\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/catalog/access/verify").contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"errada\"}"))
                .andExpect(status().isForbidden());
    }
}
