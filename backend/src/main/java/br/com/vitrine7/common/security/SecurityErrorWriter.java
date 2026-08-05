package br.com.vitrine7.common.security;

import br.com.vitrine7.common.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SecurityErrorWriter {

    private final JsonMapper jsonMapper;

    public void write(
            HttpServletRequest request,
            HttpServletResponse response,
            int status,
            String code,
            String message
    ) throws IOException {

        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiErrorResponse body = new ApiErrorResponse(
                OffsetDateTime.now(),
                status,
                code,
                message,
                request.getRequestURI(),
                List.of()
        );

        jsonMapper.writeValue(response.getOutputStream(), body);
    }
}
