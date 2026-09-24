package org.example.study.admin.api;

import java.time.Instant;

import jakarta.servlet.http.HttpServletRequest;
import org.example.study.admin.service.AdminTokenService;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理台登录、退出和当前身份接口。 */
@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AdminTokenService tokenService;
    private final MessageSource messages;

    public AdminAuthController(AdminTokenService tokenService, MessageSource messages) {
        this.tokenService = tokenService;
        this.messages = messages;
    }

    @PostMapping("/login")
    public AdminResponse<AdminLoginResponse> login(@RequestBody AdminLoginRequest body,
                                                   HttpServletRequest request) {
        return success(tokenService.login(body), request);
    }

    @PostMapping("/logout")
    public AdminResponse<Void> logout(HttpServletRequest request) {
        tokenService.logout();
        return success(null, request);
    }

    @GetMapping("/me")
    public AdminResponse<AdminMeResponse> me(HttpServletRequest request) {
        return success(tokenService.current(), request);
    }

    private <T> AdminResponse<T> success(T data, HttpServletRequest request) {
        return new AdminResponse<>("SUCCESS", messages.getMessage("admin.success", null,
                AdminLocale.resolve(request)), data, Instant.now());
    }
}
