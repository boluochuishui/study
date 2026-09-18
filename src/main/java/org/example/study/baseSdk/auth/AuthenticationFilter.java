package org.example.study.baseSdk.auth;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.study.api.ApiErrorResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 位于 Controller 之前的系统级鉴权入口。
 */
@Component
public class AuthenticationFilter extends OncePerRequestFilter {

    private final AuthRuleMatcher ruleMatcher;
    private final AuthenticationManager authenticationManager;
    private final AuthenticationLogRecorder authenticationLogRecorder;
    private final ObjectMapper objectMapper;

    public AuthenticationFilter(
            AuthRuleMatcher ruleMatcher,
            AuthenticationManager authenticationManager,
            AuthenticationLogRecorder authenticationLogRecorder,
            ObjectMapper objectMapper
    ) {
        this.ruleMatcher = ruleMatcher;
        this.authenticationManager = authenticationManager;
        this.authenticationLogRecorder = authenticationLogRecorder;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest servletRequest, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startNanos = System.nanoTime();
        String traceId = Optional.ofNullable(servletRequest.getHeader("X-Trace-Id"))
                .filter(value -> !value.isBlank())
                .orElseGet(() -> UUID.randomUUID().toString());
        CachedBodyHttpServletRequest request = new CachedBodyHttpServletRequest(servletRequest);
        String path = normalizedPath(request);
        AuthRule rule = ruleMatcher.match(request.getMethod(), path).orElse(null);
        String appId = valueOrEmpty(request.getHeader("X-App-Id"));

        try {
            if (rule == null) {
                throw new AuthenticationException("AUTH_RULE_NOT_MATCHED", "Authentication rule was not matched", 403);
            }
            AuthenticationRequest authenticationRequest = new AuthenticationRequest(
                    request.getMethod(),
                    path,
                    authenticationHeaders(request),
                    request.body(),
                    request.getRemoteAddr(),
                    "HTTP"
            );
            AuthenticationPrincipal principal = authenticationManager.authenticate(authenticationRequest, rule);
            AuthenticationContext.set(principal);
            authenticationLogRecorder.success(traceId, principal.subject(), rule,
                    ruleMatcher.currentVersion(), elapsedMillis(startNanos));
            response.setHeader("X-Trace-Id", traceId);
            filterChain.doFilter(request, response);
        } catch (AuthenticationException exception) {
            authenticationLogRecorder.failure(traceId, appId, rule, ruleMatcher.currentVersion(),
                    exception, elapsedMillis(startNanos));
            writeError(response, exception);
        } finally {
            AuthenticationContext.clear();
        }
    }

    private Map<String, String> authenticationHeaders(HttpServletRequest request) {
        return Map.of(
                "X-App-Id", valueOrEmpty(request.getHeader("X-App-Id")),
                "X-Timestamp", valueOrEmpty(request.getHeader("X-Timestamp")),
                "X-Nonce", valueOrEmpty(request.getHeader("X-Nonce")),
                "X-Signature", valueOrEmpty(request.getHeader("X-Signature"))
        );
    }

    private String normalizedPath(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.isBlank() ? "/" : path;
    }

    private void writeError(HttpServletResponse response, AuthenticationException exception) throws IOException {
        response.setStatus(exception.status());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(),
                new ApiErrorResponse(exception.errorCode(), exception.getMessage(), Instant.now()));
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
