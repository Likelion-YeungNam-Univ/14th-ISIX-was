package com.closr.global.security;

import com.closr.domain.user.entity.Session;
import com.closr.domain.user.service.SessionService;
import com.closr.global.common.ApiResponse;
import com.closr.global.exception.CustomException;
import com.closr.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SessionTokenFilter extends OncePerRequestFilter {

    public static final String SESSION_TOKEN_HEADER = "X-Session-Token";
    public static final String SESSION_ATTRIBUTE = "session";

    private static final List<String> EXCLUDED_PATHS = List.of(
            "/api/v1/sessions",
            "/api-docs",
            "/swagger-ui"
    );

    private final SessionService sessionService;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = request.getHeader(SESSION_TOKEN_HEADER);

        if (token == null || token.isBlank()) {
            writeError(response, ErrorCode.UNAUTHORIZED);
            return;
        }

        try {
            Session session = sessionService.validateSession(token);
            request.setAttribute(SESSION_ATTRIBUTE, session);
        } catch (CustomException e) {
            writeError(response, e.getErrorCode());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        response.getWriter().write(
                objectMapper.writeValueAsString(ApiResponse.fail(errorCode))
        );
    }
}
