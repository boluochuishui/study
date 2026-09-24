package org.example.study.admin.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

import org.example.study.admin.api.AdminApiException;
import org.example.study.admin.api.AdminLoginRequest;
import org.example.study.admin.api.AdminLoginResponse;
import org.example.study.admin.api.AdminMeResponse;
import org.example.study.admin.security.AdminPrincipal;
import org.example.study.admin.security.AdminSecurityContext;
import org.example.study.baseSdk.database.api.model.AdminUserData;
import org.example.study.baseSdk.database.api.service.AdminSecurityDataService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** 管理台登录、令牌签发和撤销服务。 */
@Service
public class AdminTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String DUMMY_PASSWORD_HASH =
            "$2a$12$oBNO0Di4t0QYQYpzX8HQ1OQ5O1OpJ9Br2V9zX1K5x1UH6HbxQv6EG";

    private final AdminSecurityDataService dataService;
    private final PasswordEncoder passwordEncoder;
    private final long tokenTtlHours;

    public AdminTokenService(AdminSecurityDataService dataService, PasswordEncoder passwordEncoder,
                             @Value("${admin.security.token-ttl-hours:8}") long tokenTtlHours) {
        if (tokenTtlHours < 1 || tokenTtlHours > 168) {
            throw new IllegalArgumentException("Admin token TTL hours must be between 1 and 168");
        }
        this.dataService = dataService;
        this.passwordEncoder = passwordEncoder;
        this.tokenTtlHours = tokenTtlHours;
    }

    public AdminLoginResponse login(AdminLoginRequest request) {
        if (request == null || invalidIdentifier(request.tenantCode()) || invalidIdentifier(request.username())
                || request.password() == null || request.password().isBlank()) {
            throw new AdminApiException("INVALID_CREDENTIALS", "Invalid credentials", HttpStatus.UNAUTHORIZED);
        }
        AdminUserData user = dataService.findUserForLogin(request.tenantCode(), request.username()).orElse(null);
        String passwordHash = user == null ? DUMMY_PASSWORD_HASH : user.passwordHash();
        if (!passwordEncoder.matches(request.password(), passwordHash) || user == null) {
            throw invalidCredentials();
        }
        String token = generateToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(tokenTtlHours);
        dataService.saveAccessToken(user.tenantId(), user.id(), sha256(token), expiresAt);
        var identity = dataService.findIdentityByTokenHash(sha256(token), LocalDateTime.now())
                .orElseThrow(() -> new IllegalStateException("Issued admin token cannot be resolved"));
        return new AdminLoginResponse(token, expiresAt, identity.tenantId(), identity.tenantCode(),
                identity.userId(), identity.username(), identity.permissions());
    }

    public void logout() {
        dataService.revokeAccessToken(AdminSecurityContext.requirePrincipal().tokenHash());
    }

    public AdminMeResponse current() {
        AdminPrincipal principal = AdminSecurityContext.requirePrincipal();
        return new AdminMeResponse(principal.tenantId(), principal.tenantCode(), principal.userId(),
                principal.username(), principal.displayName(), principal.permissions());
    }

    public static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }

    private String generateToken() {
        byte[] value = new byte[32];
        RANDOM.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private boolean invalidIdentifier(String value) {
        return value == null || !value.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");
    }

    private AdminApiException invalidCredentials() {
        return new AdminApiException("INVALID_CREDENTIALS", "Invalid credentials", HttpStatus.UNAUTHORIZED);
    }
}
