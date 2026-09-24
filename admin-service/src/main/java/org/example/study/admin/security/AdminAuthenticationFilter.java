package org.example.study.admin.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.study.admin.api.AdminLocale;
import org.example.study.admin.api.AdminResponse;
import org.example.study.admin.service.AdminTokenService;
import org.example.study.baseSdk.database.api.model.AdminIdentityData;
import org.example.study.baseSdk.database.api.service.AdminSecurityDataService;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/** 从不透明访问令牌恢复租户身份和实时权限。 */
@Component
public class AdminAuthenticationFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/admin/auth/login";

    private final AdminSecurityDataService dataService;
    private final MessageSource messages;
    private final ObjectMapper objectMapper;

    public AdminAuthenticationFilter(AdminSecurityDataService dataService, MessageSource messages,
                                     ObjectMapper objectMapper) {
        this.dataService = dataService;
        this.messages = messages;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/admin/")
                || LOGIN_PATH.equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = bearerToken(request);
        String tokenHash = token == null ? "" : AdminTokenService.sha256(token);
        AdminIdentityData identity = tokenHash.isEmpty() ? null
                : dataService.findIdentityByTokenHash(tokenHash, LocalDateTime.now()).orElse(null);
        if (identity == null) {
            unauthorized(request, response);
            return;
        }
        AdminSecurityContext.set(new AdminPrincipal(identity.userId(), identity.tenantId(), identity.tenantCode(),
                identity.username(), identity.displayName(), tokenHash, identity.permissions()));
        try {
            chain.doFilter(request, response);
        } finally {
            AdminSecurityContext.clear();
        }
    }

    private String bearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        String token = authorization.substring(7).trim();
        return token.isEmpty() ? null : token;
    }

    private void unauthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), new AdminResponse<>("UNAUTHORIZED",
                messages.getMessage("admin.error.UNAUTHORIZED", null, AdminLocale.resolve(request)), null,
                java.time.Instant.now()));
    }
}
