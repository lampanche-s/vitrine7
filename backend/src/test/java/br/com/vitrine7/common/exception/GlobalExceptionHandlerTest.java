package br.com.vitrine7.common.exception;

import br.com.vitrine7.common.response.ApiErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void returnsNotFoundForUnknownRoute() {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/api/v1/definitely-not-a-route"
        );

        ResponseEntity<ApiErrorResponse> response = handler.handleNoResourceFound(
                mock(NoResourceFoundException.class),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        assertThat(response.getBody())
                .isNotNull()
                .satisfies(body -> {
                    assertThat(body.status()).isEqualTo(404);
                    assertThat(body.code()).isEqualTo("ROUTE_NOT_FOUND");
                    assertThat(body.message()).isEqualTo("A rota solicitada não existe.");
                    assertThat(body.path()).isEqualTo("/api/v1/definitely-not-a-route");
                    assertThat(body.fieldErrors()).isEmpty();
                });
    }
}
