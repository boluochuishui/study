package org.example.study.admin;

import org.example.study.admin.api.AdminConfigController;
import org.example.study.admin.api.AdminExceptionHandler;
import org.example.study.admin.security.AdminAuthenticationFilter;
import org.example.study.admin.security.AdminAuthorizationInterceptor;
import org.example.study.admin.security.AdminPermissions;
import org.example.study.admin.service.AdminConfigService;
import org.example.study.baseSdk.database.api.exception.ConfigConflictException;
import org.example.study.baseSdk.database.api.model.ConfigItemData;
import org.example.study.baseSdk.database.api.model.AdminIdentityData;
import org.example.study.baseSdk.database.api.model.SaveConfigItemCommand;
import org.example.study.baseSdk.database.api.service.ConfigDataService;
import org.example.study.baseSdk.database.api.service.AdminSecurityDataService;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import java.util.Locale;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * 覆盖管理台鉴权、国际化与版本冲突。
 */
class AdminConfigApiTests {

    private final ConfigDataService data = mock(ConfigDataService.class);
    private final AdminSecurityDataService securityData = mock(AdminSecurityDataService.class);
    private final MockMvc mvc = createMvc();

    @Test
    void requiresAdminTokenAndLocalizesUnauthorized() throws Exception {
        var response = mvc.perform(get("/api/admin/configs/demo/key")
                        .header("Accept-Language", "zh-CN"))
                .andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("UNAUTHORIZED", "未授权访问");
    }

    @Test
    void rejectsWriteWithoutPermission() throws Exception {
        MockMvc readOnlyMvc = createMvc(AdminPermissions.CONFIG_READ);
        var response = readOnlyMvc.perform(put("/api/admin/configs/demo/key")
                        .header("Authorization", "Bearer valid-token")
                        .contentType("application/json")
                        .content("{\"value\":\"new\",\"enabled\":true,\"version\":3}"))
                .andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("FORBIDDEN");
    }

    @Test
    void listsConfigurationsWithoutLanguageHeaderInChinese() throws Exception {
        when(data.list(7, "demo")).thenReturn(List.of(item(3, false)));

        var response = mvc.perform(get("/api/admin/configs")
                        .param("namespace", "demo")
                        .header("Authorization", "Bearer test-admin-token"))
                .andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).contains("操作成功", "\"enabled\":false");
    }

    @Test
    void rejectsInvalidConfigRequest() throws Exception {
        var response = mvc.perform(put("/api/admin/configs/demo/key")
                        .header("Authorization", "Bearer test-admin-token")
                        .contentType("application/json")
                        .content("{\"value\":\"\",\"enabled\":true,\"version\":0}"))
                .andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentAsString()).contains("INVALID_REQUEST");
    }

    @Test
    void readsDisabledItemInEnglish() throws Exception {
        when(data.find(7, "demo", "key")).thenReturn(Optional.of(item(3, false)));

        var response = mvc.perform(get("/api/admin/configs/demo/key")
                        .header("Authorization", "Bearer test-admin-token")
                        .header("Accept-Language", "en-US")
                        .locale(Locale.US))
                .andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).contains("Success", "\"enabled\":false", "\"version\":3");
    }

    @Test
    void savesWithExpectedVersionAndChineseMessage() throws Exception {
        when(data.saveIfVersion(any(SaveConfigItemCommand.class), eq(3L))).thenReturn(item(4, true));

        var response = mvc.perform(put("/api/admin/configs/demo/key")
                        .header("Authorization", "Bearer test-admin-token")
                        .header("Accept-Language", "zh-CN")
                        .contentType("application/json")
                        .content("{\"value\":\"new\",\"enabled\":true,\"version\":3}"))
                .andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).contains("操作成功", "\"version\":4");
        verify(data).saveIfVersion(any(SaveConfigItemCommand.class), eq(3L));
    }

    @Test
    void returnsConflictForStaleVersion() throws Exception {
        when(data.saveIfVersion(any(SaveConfigItemCommand.class), eq(2L))).thenThrow(new ConfigConflictException());

        var response = mvc.perform(put("/api/admin/configs/demo/key")
                        .header("Authorization", "Bearer test-admin-token")
                        .contentType("application/json")
                        .content("{\"value\":\"new\",\"enabled\":true,\"version\":2}"))
                .andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(409);
        assertThat(response.getContentAsString()).contains("CONFIG_CONFLICT");
    }

    @Test
    void disablePreservesValueAndChecksVersion() throws Exception {
        when(data.find(7, "demo", "key")).thenReturn(Optional.of(item(3, true)));
        when(data.saveIfVersion(any(SaveConfigItemCommand.class), eq(3L))).thenReturn(item(4, false));

        var response = mvc.perform(post("/api/admin/configs/demo/key/disable")
                        .header("Authorization", "Bearer test-admin-token")
                        .contentType("application/json")
                        .content("{\"version\":3}"))
                .andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).contains("\"enabled\":false");
    }

    private MockMvc createMvc(String... permissions) {
        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename("messages");
        messages.setDefaultEncoding("UTF-8");
        Set<String> granted = permissions.length == 0
                ? Set.of(AdminPermissions.CONFIG_READ, AdminPermissions.CONFIG_WRITE) : Set.of(permissions);
        when(securityData.findIdentityByTokenHash(any(String.class), any(LocalDateTime.class)))
                .thenReturn(Optional.of(new AdminIdentityData(9, 7, "tenant-a", "admin", "Admin", granted)));
        AdminConfigService service = new AdminConfigService(data);
        return MockMvcBuilders.standaloneSetup(new AdminConfigController(service, messages))
                .setControllerAdvice(new AdminExceptionHandler(messages))
                .addInterceptors(new AdminAuthorizationInterceptor())
                .addFilters(new AdminAuthenticationFilter(securityData, messages, new ObjectMapper()))
                .build();
    }

    private ConfigItemData item(long version, boolean enabled) {
        return new ConfigItemData(1L, 7L, "demo", "key", "new", version, enabled,
                "test", LocalDateTime.now(), LocalDateTime.now());
    }
}
