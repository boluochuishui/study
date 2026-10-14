package org.example.study.admin.security;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.example.study.admin.api.AdminApiException;
import org.example.study.admin.api.SaveManagedResourceRequest;
import org.example.study.admin.service.AdminManagedResourceService;
import org.example.study.baseSdk.database.api.model.ManagedResource;
import org.example.study.baseSdk.database.api.model.SpelRuleReleaseData;
import org.example.study.baseSdk.database.api.service.ManagedResourceDataService;
import org.example.study.baseSdk.database.api.service.SpelRuleDataService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 管理表单的字段与租户关联校验。 */
class AdminManagedResourceServiceTests {
    private final ManagedResourceDataService data = mock(ManagedResourceDataService.class);
    private final SpelRuleDataService spel = mock(SpelRuleDataService.class);
    private final AdminManagedResourceService service = new AdminManagedResourceService(data, spel);

    @BeforeEach
    void openIdentity() {
        AdminSecurityContext.set(new AdminPrincipal(17, 7, "tenant", "tester", "Tester", "hash",
                Set.of("CONFIG_WRITE")));
    }

    @AfterEach
    void closeIdentity() {
        AdminSecurityContext.clear();
    }

    @Test
    void appIdIsAcceptedAsTextAndTenantComesFromLogin() {
        service.create("apps", request(0, Map.of("app_id", "client-a", "app_name", "Client A",
                "credential_ref", "env:CLIENT_A_SECRET", "enabled", true)));
        verify(data).create(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq(ManagedResource.APPS), anyMap());
    }

    @Test
    void sceneCannotReferenceAppFromAnotherTenant() {
        when(data.find(7, ManagedResource.APPS, 9)).thenReturn(null);
        assertThatThrownBy(() -> service.create("scenes", request(0, Map.of(
                "app_record_id", 9, "scene_code", "comment", "scene_name", "Comment"))))
                .isInstanceOf(AdminApiException.class);
    }

    @Test
    void membershipRequiresSameContentTypeAndReadyAsset() {
        when(data.find(7, ManagedResource.LIBRARIES, 1)).thenReturn(Map.of(
                "id", 1L, "enabled", 1, "content_type", "TEXT"));
        when(data.find(7, ManagedResource.ASSETS, 2)).thenReturn(Map.of(
                "id", 2L, "enabled", 1, "content_type", "IMAGE", "storage_status", "READY"));
        assertThatThrownBy(() -> service.create("memberships", request(0, Map.of(
                "library_id", 1, "asset_id", 2))))
                .isInstanceOf(AdminApiException.class);
    }

    @Test
    void synchronousLlmRuleIsRejected() {
        when(data.find(7, ManagedResource.SCENES, 3)).thenReturn(Map.of("id", 3L, "enabled", 1));
        assertThatThrownBy(() -> service.create("rules", request(0, Map.of(
                "scene_id", 3, "rule_code", "llm", "rule_name", "Model",
                "content_type", "TEXT", "rule_type", "LLM", "execution_mode", "SYNC",
                "on_hit_action", "REVIEW", "parameters_json", "{}"))))
                .isInstanceOf(AdminApiException.class);
    }

    @Test
    void spelRuleMustExistInPublishedSnapshot() {
        when(data.find(7, ManagedResource.SCENES, 3)).thenReturn(Map.of("id", 3L, "enabled", 1));
        when(spel.findReleaseById(7, 5)).thenReturn(Optional.of(new SpelRuleReleaseData(
                5, 7, "text.default", 1, "TEXT",
                "{\"scene\":\"TEXT\",\"enabled\":true,\"rules\":[{\"ruleId\":\"known\"}]}",
                "checksum", false, "tester", null)));
        assertThatThrownBy(() -> service.create("rules", request(0, Map.of(
                "scene_id", 3, "rule_code", "spel", "rule_name", "Expression",
                "content_type", "TEXT", "rule_type", "SPEL", "execution_mode", "BOTH",
                "spel_release_id", 5, "spel_rule_id", "missing"))))
                .isInstanceOf(AdminApiException.class);
        service.create("rules", request(0, Map.of(
                "scene_id", 3, "rule_code", "spel", "rule_name", "Expression",
                "content_type", "TEXT", "rule_type", "SPEL", "execution_mode", "BOTH",
                "spel_release_id", 5, "spel_rule_id", "known")));
        verify(data).create(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq(ManagedResource.RULES), anyMap());
    }

    private SaveManagedResourceRequest request(long version, Map<String, Object> values) {
        SaveManagedResourceRequest body = new SaveManagedResourceRequest();
        body.setVersion(version);
        body.setValues(values);
        return body;
    }
}
