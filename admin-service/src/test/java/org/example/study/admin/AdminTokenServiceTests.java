package org.example.study.admin;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import org.example.study.admin.api.AdminApiException;
import org.example.study.admin.api.AdminLoginRequest;
import org.example.study.admin.service.AdminTokenService;
import org.example.study.baseSdk.database.api.model.AdminIdentityData;
import org.example.study.baseSdk.database.api.model.AdminUserData;
import org.example.study.baseSdk.database.api.service.AdminSecurityDataService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 管理台令牌签发测试。 */
class AdminTokenServiceTests {

    private final AdminSecurityDataService dataService = mock(AdminSecurityDataService.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final AdminTokenService service = new AdminTokenService(dataService, passwordEncoder, 8);

    @Test
    void issuesOpaqueTokenAndPersistsOnlyItsHash() {
        String passwordHash = passwordEncoder.encode("StrongPassword123");
        var user = new AdminUserData(11L, 7, "tenant-a", "alice", "Alice", passwordHash, true);
        var identity = new AdminIdentityData(11, 7, "tenant-a", "alice", "Alice", Set.of("CONFIG_READ"));
        when(dataService.findUserForLogin("tenant-a", "alice")).thenReturn(Optional.of(user));
        when(dataService.findIdentityByTokenHash(any(), any())).thenReturn(Optional.of(identity));

        var response = service.login(new AdminLoginRequest("tenant-a", "alice", "StrongPassword123"));

        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(dataService).saveAccessToken(eq(7L), eq(11L), hashCaptor.capture(), any(LocalDateTime.class));
        assertThat(response.accessToken()).hasSize(43);
        assertThat(hashCaptor.getValue()).isEqualTo(AdminTokenService.sha256(response.accessToken()));
        assertThat(hashCaptor.getValue()).doesNotContain(response.accessToken());
        assertThat(response.permissions()).containsExactly("CONFIG_READ");
    }

    @Test
    void rejectsInvalidPasswordWithoutIssuingToken() {
        String passwordHash = passwordEncoder.encode("StrongPassword123");
        var user = new AdminUserData(11L, 7, "tenant-a", "alice", "Alice", passwordHash, true);
        when(dataService.findUserForLogin("tenant-a", "alice")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.login(new AdminLoginRequest("tenant-a", "alice", "wrong")))
                .isInstanceOf(AdminApiException.class)
                .hasMessage("Invalid credentials");
        verify(dataService, never()).saveAccessToken(anyLong(), anyLong(), any(), any());
    }

    @Test
    void rejectsUnknownUserWithTheSamePublicError() {
        when(dataService.findUserForLogin("tenant-a", "unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new AdminLoginRequest("tenant-a", "unknown", "password")))
                .isInstanceOf(AdminApiException.class)
                .hasMessage("Invalid credentials");
    }
}
