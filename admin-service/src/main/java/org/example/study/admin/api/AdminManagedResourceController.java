package org.example.study.admin.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.example.study.admin.security.AdminPermissions;
import org.example.study.admin.security.RequireAdminPermission;
import org.example.study.admin.service.AdminManagedResourceService;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** App、场景、规则和媒资配置接口。 */
@RestController
@RequestMapping("/api/admin/managed")
public class AdminManagedResourceController {
    private final AdminManagedResourceService service;
    private final MessageSource messages;

    public AdminManagedResourceController(AdminManagedResourceService service, MessageSource messages) {
        this.service = service;
        this.messages = messages;
    }

    @GetMapping("/{resource}")
    @RequireAdminPermission(AdminPermissions.CONFIG_READ)
    public AdminResponse<List<Map<String, Object>>> list(@PathVariable String resource, HttpServletRequest request) {
        return success(service.list(resource), request);
    }

    @PostMapping("/{resource}")
    @RequireAdminPermission(AdminPermissions.CONFIG_WRITE)
    public AdminResponse<Map<String, Object>> create(@PathVariable String resource,
                                                     @RequestBody SaveManagedResourceRequest body,
                                                     HttpServletRequest request) {
        return success(service.create(resource, body), request);
    }

    @PutMapping("/{resource}/{id}")
    @RequireAdminPermission(AdminPermissions.CONFIG_WRITE)
    public AdminResponse<Map<String, Object>> update(@PathVariable String resource, @PathVariable long id,
                                                     @RequestBody SaveManagedResourceRequest body,
                                                     HttpServletRequest request) {
        return success(service.update(resource, id, body), request);
    }

    private <T> AdminResponse<T> success(T data, HttpServletRequest request) {
        return new AdminResponse<>("SUCCESS", messages.getMessage("admin.success", null, AdminLocale.resolve(request)),
                data, Instant.now());
    }
}
