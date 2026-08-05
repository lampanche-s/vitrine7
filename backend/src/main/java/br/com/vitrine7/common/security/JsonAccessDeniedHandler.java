package br.com.vitrine7.common.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JsonAccessDeniedHandler
        implements AccessDeniedHandler {

    private final SecurityErrorWriter securityErrorWriter;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {


        securityErrorWriter.write(
                request,
                response,
                HttpServletResponse.SC_FORBIDDEN,
                "ACCESS_DENIED",
                "Você não tem permissão para executar esta ação."
        );
    }
}
