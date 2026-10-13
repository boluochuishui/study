package org.example.study.admin.api;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.example.study.admin.security.AdminPermissions;
import org.example.study.admin.security.RequireAdminPermission;
import org.example.study.admin.service.AdminOnlineTestService;
import org.example.study.baseSdk.database.api.model.OnlineTestRecordData;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 在线测试经管理台服务端代签后进入检测接入服务。 */
@RestController
@RequestMapping("/api/admin/online-tests")
public class AdminOnlineTestController {
    private final AdminOnlineTestService service;
    private final MessageSource messages;

    public AdminOnlineTestController(AdminOnlineTestService service, MessageSource messages) {
        this.service = service;
        this.messages = messages;
    }

    @GetMapping("/profiles/{contentType}")
    @RequireAdminPermission(AdminPermissions.TEST_RUN)
    public AdminResponse<OnlineTestProfile> profile(@PathVariable String contentType,
                                                     HttpServletRequest request) {
        return success(service.profile(contentType), request);
    }

    @PostMapping
    @RequireAdminPermission(AdminPermissions.TEST_RUN)
    public AdminResponse<OnlineTestRunResult> run(@RequestBody OnlineTestRequest body,
                                                  HttpServletRequest request) {
        return success(service.run(body), request);
    }

    @GetMapping
    @RequireAdminPermission(AdminPermissions.TEST_READ)
    public AdminResponse<List<OnlineTestRecordData>> records(HttpServletRequest request) {
        return success(service.records(), request);
    }

    private <T> AdminResponse<T> success(T data, HttpServletRequest request) {
        return new AdminResponse<>("SUCCESS",
                messages.getMessage("admin.success", null, AdminLocale.resolve(request)), data, Instant.now());
    }
}
