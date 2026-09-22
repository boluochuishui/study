package org.example.study.admin.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;
import org.example.study.admin.api.AdminResponse;
import org.example.study.admin.api.AdminLocale;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

/**
 * 管理台独立认证边界，启动时必须配置令牌。
 */
@Component
public class AdminTokenFilter extends OncePerRequestFilter {

    private final byte[] expectedToken;
    private final MessageSource messages;
    private final ObjectMapper objectMapper;

    public AdminTokenFilter(@Value("${admin.token:}") String token, MessageSource messages, ObjectMapper objectMapper) {
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Admin token must be configured");
        }
        this.expectedToken = token.getBytes(StandardCharsets.UTF_8);
        this.messages = messages;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        byte[] supplied = authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring(7).getBytes(StandardCharsets.UTF_8) : new byte[0];
        if (!MessageDigest.isEqual(expectedToken, supplied)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(response.getOutputStream(), new AdminResponse<>(
                    "UNAUTHORIZED", messages.getMessage("admin.error.UNAUTHORIZED", null, AdminLocale.resolve(request)),
                    null, Instant.now()));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
