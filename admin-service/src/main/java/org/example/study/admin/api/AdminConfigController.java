package org.example.study.admin.api;

import jakarta.servlet.http.HttpServletRequest;
import org.example.study.admin.service.AdminConfigService;
import org.example.study.baseSdk.database.api.model.ConfigItemData;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * 配置管理 API，消息按请求语言返回。
 */
@RestController
@RequestMapping("/api/admin/configs")
public class AdminConfigController {

    private final AdminConfigService service;
    private final MessageSource messages;

    public AdminConfigController(AdminConfigService service, MessageSource messages) {
        this.service = service;
        this.messages = messages;
    }

    @GetMapping
    public AdminResponse<List<ConfigItemData>> list(@RequestParam String namespace, HttpServletRequest request) {
        return success(service.list(namespace), request);
    }

    @GetMapping("/{namespace}/{key}")
    public AdminResponse<ConfigItemData> get(@PathVariable String namespace, @PathVariable String key,
                                             HttpServletRequest request) {
        return success(service.get(namespace, key), request);
    }

    @PutMapping("/{namespace}/{key}")
    public AdminResponse<ConfigItemData> save(@PathVariable String namespace, @PathVariable String key,
                                              @RequestBody SaveConfigRequest body, HttpServletRequest request) {
        return success(service.save(namespace, key, body), request);
    }

    @PostMapping("/{namespace}/{key}/disable")
    public AdminResponse<ConfigItemData> disable(@PathVariable String namespace, @PathVariable String key,
                                                 @RequestBody VersionRequest body, HttpServletRequest request) {
        return success(service.disable(namespace, key, body == null ? null : body.version()), request);
    }

    private <T> AdminResponse<T> success(T data, HttpServletRequest request) {
        return new AdminResponse<>("SUCCESS", messages.getMessage("admin.success", null, AdminLocale.resolve(request)), data, Instant.now());
    }
}
