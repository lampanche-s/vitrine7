package br.com.vitrine7.report.controller;

import br.com.vitrine7.report.service.ReportPeriodAccessService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportAccessControllerTest {

    private final ReportPeriodAccessService accessService =
            mock(ReportPeriodAccessService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ReportAccessController(accessService))
            .build();

    @Test
    void returnsNoContentForCorrectPassword() throws Exception {
        when(accessService.isPasswordValid("segredo")).thenReturn(true);

        mockMvc.perform(post("/api/v1/reports/access/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"segredo\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void returnsForbiddenForWrongPassword() throws Exception {
        when(accessService.isPasswordValid("incorreta")).thenReturn(false);

        mockMvc.perform(post("/api/v1/reports/access/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"incorreta\"}"))
                .andExpect(status().isForbidden());
    }
}
