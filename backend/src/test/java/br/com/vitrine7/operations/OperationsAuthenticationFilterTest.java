package br.com.vitrine7.operations;

import br.com.vitrine7.common.security.SecurityErrorWriter;
import br.com.vitrine7.report.service.ReportPeriodAccessService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OperationsAuthenticationFilterTest {
    private final ReportPeriodAccessService credentials =
            mock(ReportPeriodAccessService.class);
    private final SecurityErrorWriter errors = mock(SecurityErrorWriter.class);
    private final OperationsAuthenticationFilter filter =
            new OperationsAuthenticationFilter(credentials, errors);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void grantsOnlyOperationalStatusAuthorityForValidCredential()
            throws ServletException, IOException {
        MockHttpServletRequest request = request("correct");
        AtomicReference<Authentication> observed = new AtomicReference<>();
        FilterChain chain = mock(FilterChain.class);
        when(credentials.isPasswordValid("correct")).thenReturn(true);

        MockHttpServletResponse response = new MockHttpServletResponse();
        doAnswer(invocation -> {
            observed.set(SecurityContextHolder.getContext().getAuthentication());
            return null;
        }).when(chain).doFilter(request, response);
        filter.doFilter(request, response, chain);

        assertEquals(
                "operations:agents-status",
                observed.get().getAuthorities().iterator().next().getAuthority()
        );
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void rejectsInvalidCredentialBeforeController()
            throws ServletException, IOException {
        MockHttpServletRequest request = request("wrong");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(credentials.isPasswordValid("wrong")).thenReturn(false);

        filter.doFilter(request, response, mock(FilterChain.class));

        verify(errors).write(
                request,
                response,
                401,
                "OPERATIONS_UNAUTHORIZED",
                "Credencial operacional invalida."
        );
    }

    private MockHttpServletRequest request(String credential) {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/api/v1/operations/agents/status"
        );
        request.addHeader(
                OperationsAuthenticationFilter.CREDENTIAL_HEADER,
                credential
        );
        return request;
    }
}
