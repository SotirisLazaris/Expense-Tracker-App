package com.portfolio.expensetracker.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.expensetracker.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/** Returns JSON (instead of an HTML page or an empty body) for 401 and 403 responses. */
@Component
public class RestSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestSecurityErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        write(response, request, HttpStatus.UNAUTHORIZED, "Authentication required: provide a valid Bearer token");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        write(response, request, HttpStatus.FORBIDDEN, "You do not have permission to access this resource");
    }

    private void write(HttpServletResponse response, HttpServletRequest request, HttpStatus status, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(),
                message, request.getRequestURI(), null);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
