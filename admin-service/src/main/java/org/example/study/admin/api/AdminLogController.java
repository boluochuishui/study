package org.example.study.admin.api;

import jakarta.servlet.http.HttpServletRequest;
import org.example.study.admin.security.AdminPermissions;
import org.example.study.admin.security.RequireAdminPermission;
import org.example.study.admin.service.AdminLogSearchService;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/** 管理台日志检索接口。 */
@RestController
@RequestMapping("/api/admin/logs")
@RequireAdminPermission(AdminPermissions.LOG_READ)
public class AdminLogController {

    private final AdminLogSearchService service;
    private final MessageSource messages;

    public AdminLogController(AdminLogSearchService service, MessageSource messages) {
        this.service = service;
        this.messages = messages;
    }

    @GetMapping
    public AdminResponse<Map<String, Object>> search(@RequestParam Instant from, @RequestParam Instant to,
                                                       @RequestParam(defaultValue = "50") int size,
                                                       @RequestParam(required = false) String cursor,
                                                       @RequestParam(required = false) String taskId,
                                                       @RequestParam(required = false) String traceId,
                                                       @RequestParam(required = false) String appId,
                                                       @RequestParam(required = false) String serviceName,
                                                       @RequestParam(required = false) String logSpace,
                                                       @RequestParam(required = false) String level,
                                                       @RequestParam(required = false) Boolean success,
                                                       HttpServletRequest request) {
        return success(service.search(from, to, size, cursor, taskId, traceId, appId, serviceName,
                logSpace, level, success), request);
    }

    @GetMapping("/tasks/{taskId}")
    public AdminResponse<Map<String, Object>> task(@PathVariable String taskId,
                                                     @RequestParam Instant from, @RequestParam Instant to,
                                                     HttpServletRequest request) {
        return success(service.task(taskId, from, to), request);
    }

    private AdminResponse<Map<String, Object>> success(Map<String, Object> data, HttpServletRequest request) {
        return new AdminResponse<>("SUCCESS", messages.getMessage("admin.success", null, AdminLocale.resolve(request)),
                data, Instant.now());
    }
}
