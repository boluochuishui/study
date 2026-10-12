package org.example.study.admin;

import com.sun.net.httpserver.HttpServer;
import org.example.study.admin.api.AdminExceptionHandler;
import org.example.study.admin.api.AdminLogController;
import org.example.study.admin.security.AdminAuthenticationFilter;
import org.example.study.admin.security.AdminAuthorizationInterceptor;
import org.example.study.admin.security.AdminPermissions;
import org.example.study.admin.service.AdminLogSearchService;
import org.example.study.baseSdk.database.api.model.AdminIdentityData;
import org.example.study.baseSdk.database.api.service.AdminSecurityDataService;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** 覆盖日志查询租户隔离、权限和参数限制。 */
class AdminLogApiTests {

    private static final String FROM = "2026-10-11T00:00:00Z";
    private static final String TO = "2026-10-12T00:00:00Z";

    @Test
    void enforcesTenantFilterAndReturnsOnlySelectedFields() throws Exception {
        List<String> queries = new ArrayList<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/logs-contentrisk-*/_search", exchange -> {
            queries.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = ("{\"hits\":{\"hits\":[{\"_source\":{\"eventId\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\","
                    + "\"taskId\":\"task-1\",\"message\":\"done\",\"secret\":\"hidden\"},"
                    + "\"sort\":[\"2026-10-11T12:00:00Z\",\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"]},"
                    + "{\"_source\":{\"eventId\":\"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb\"},"
                    + "\"sort\":[\"2026-10-11T11:00:00Z\",\"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb\"]}]}}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (var output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
        server.start();
        try {
            MockMvc mvc = mvc("http://127.0.0.1:" + server.getAddress().getPort(),
                    Set.of(AdminPermissions.LOG_READ));
            var response = mvc.perform(get("/api/admin/logs")
                            .param("from", FROM).param("to", TO).param("taskId", "task-1").param("size", "1")
                            .header("Authorization", "Bearer valid-token"))
                    .andReturn().getResponse();
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getContentAsString()).contains("task-1", "done").doesNotContain("hidden");
            assertThat(queries.get(0)).contains("\"tenantId\":7", "\"taskId\":\"task-1\"");
            String cursor = new ObjectMapper().readTree(response.getContentAsString())
                    .get("data").get("nextCursor").asText();
            assertThat(cursor).isNotBlank();
            mvc.perform(get("/api/admin/logs").param("from", FROM).param("to", TO)
                    .param("size", "1").param("cursor", cursor)
                    .header("Authorization", "Bearer valid-token")).andReturn();
            assertThat(queries.get(1)).contains("search_after", "\"tenantId\":7");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsUserWithoutPermissionAndInvalidRange() throws Exception {
        MockMvc forbidden = mvc("", Set.of(AdminPermissions.CONFIG_READ));
        var response = forbidden.perform(get("/api/admin/logs")
                        .param("from", FROM).param("to", TO)
                        .header("Authorization", "Bearer valid-token"))
                .andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(403);

        MockMvc allowed = mvc("", Set.of(AdminPermissions.LOG_READ));
        response = allowed.perform(get("/api/admin/logs")
                        .param("from", TO).param("to", FROM)
                        .header("Authorization", "Bearer valid-token"))
                .andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(400);
    }

    private MockMvc mvc(String url, Set<String> permissions) {
        AdminSecurityDataService securityData = mock(AdminSecurityDataService.class);
        when(securityData.findIdentityByTokenHash(any(String.class), any(LocalDateTime.class)))
                .thenReturn(Optional.of(new AdminIdentityData(9, 7, "tenant-a", "admin", "Admin", permissions)));
        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename("messages");
        messages.setDefaultEncoding("UTF-8");
        AdminLogSearchService service = new AdminLogSearchService(url, "", "logs-contentrisk-*");
        return MockMvcBuilders.standaloneSetup(new AdminLogController(service, messages))
                .setControllerAdvice(new AdminExceptionHandler(messages))
                .addInterceptors(new AdminAuthorizationInterceptor())
                .addFilters(new AdminAuthenticationFilter(securityData, messages, new ObjectMapper()))
                .build();
    }
}
