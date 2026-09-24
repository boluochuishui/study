package org.example.study.admin.api;

import java.time.Instant;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.example.study.admin.service.AdminSpelRuleService;
import org.example.study.admin.security.AdminPermissions;
import org.example.study.admin.security.RequireAdminPermission;
import org.example.study.baseSdk.database.api.model.SpelRuleDraftData;
import org.example.study.baseSdk.database.api.model.SpelRuleReleaseData;
import org.example.study.baseSdk.database.api.model.SpelRuleSetData;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** SpEL 规则草稿、校验和发布管理接口。 */
@RestController
@RequestMapping("/api/admin/rule-sets")
public class AdminSpelRuleController {

    private final AdminSpelRuleService service;
    private final MessageSource messages;

    public AdminSpelRuleController(AdminSpelRuleService service, MessageSource messages) {
        this.service = service;
        this.messages = messages;
    }

    @GetMapping
    @RequireAdminPermission(AdminPermissions.RULE_READ)
    public AdminResponse<List<SpelRuleSetData>> list(HttpServletRequest request) {
        return success(service.listRuleSets(), request);
    }

    @PutMapping("/{code}")
    @RequireAdminPermission(AdminPermissions.RULE_WRITE)
    public AdminResponse<SpelRuleSetData> saveRuleSet(@PathVariable String code,
                                                      @RequestBody SaveRuleSetRequest body,
                                                      HttpServletRequest request) {
        return success(service.saveRuleSet(code, body), request);
    }

    @GetMapping("/{code}/rules")
    @RequireAdminPermission(AdminPermissions.RULE_READ)
    public AdminResponse<List<SpelRuleDraftData>> listRules(@PathVariable String code,
                                                            HttpServletRequest request) {
        return success(service.listRules(code), request);
    }

    @PutMapping("/{code}/rules/{ruleId}")
    @RequireAdminPermission(AdminPermissions.RULE_WRITE)
    public AdminResponse<SpelRuleDraftData> saveRule(@PathVariable String code, @PathVariable String ruleId,
                                                     @RequestBody SaveSpelRuleRequest body,
                                                     HttpServletRequest request) {
        return success(service.saveRule(code, ruleId, body), request);
    }

    @PostMapping("/{code}/validate")
    @RequireAdminPermission(AdminPermissions.RULE_READ)
    public AdminResponse<RuleValidationResult> validate(@PathVariable String code, HttpServletRequest request) {
        return success(service.validate(code), request);
    }

    @PostMapping("/{code}/publish")
    @RequireAdminPermission(AdminPermissions.RULE_PUBLISH)
    public AdminResponse<SpelRuleReleaseData> publish(@PathVariable String code,
                                                      @RequestBody PublishRuleSetRequest body,
                                                      HttpServletRequest request) {
        return success(service.publish(code, body), request);
    }

    @GetMapping("/{code}/releases")
    @RequireAdminPermission(AdminPermissions.RULE_READ)
    public AdminResponse<List<SpelRuleReleaseData>> releases(@PathVariable String code,
                                                              HttpServletRequest request) {
        return success(service.listReleases(code), request);
    }

    @PostMapping("/{code}/rollback/{releaseVersion}")
    @RequireAdminPermission(AdminPermissions.RULE_PUBLISH)
    public AdminResponse<SpelRuleReleaseData> rollback(@PathVariable String code,
                                                       @PathVariable long releaseVersion,
                                                       @RequestBody PublishRuleSetRequest body,
                                                       HttpServletRequest request) {
        return success(service.rollback(code, releaseVersion, body), request);
    }

    private <T> AdminResponse<T> success(T data, HttpServletRequest request) {
        return new AdminResponse<>("SUCCESS", messages.getMessage("admin.success", null,
                AdminLocale.resolve(request)), data, Instant.now());
    }
}
