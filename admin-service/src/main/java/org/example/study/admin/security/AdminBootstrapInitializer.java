package org.example.study.admin.security;

import org.example.study.baseSdk.database.api.service.AdminSecurityDataService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 首次启动时从环境变量创建默认租户管理员。 */
@Component
public class AdminBootstrapInitializer implements ApplicationRunner {

    private final AdminSecurityDataService dataService;
    private final PasswordEncoder passwordEncoder;
    private final String tenantCode;
    private final String username;
    private final String password;

    public AdminBootstrapInitializer(AdminSecurityDataService dataService, PasswordEncoder passwordEncoder,
                                     @Value("${admin.bootstrap.tenant-code:default}") String tenantCode,
                                     @Value("${admin.bootstrap.username:admin}") String username,
                                     @Value("${admin.bootstrap.password:}") String password) {
        this.dataService = dataService;
        this.passwordEncoder = passwordEncoder;
        this.tenantCode = tenantCode;
        this.username = username;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (dataService.countUsers(tenantCode) > 0) {
            return;
        }
        if (password == null || password.length() < 12) {
            throw new IllegalStateException("Admin bootstrap password must contain at least 12 characters");
        }
        long tenantId = dataService.requireTenantId(tenantCode);
        var user = dataService.createUser(tenantId, username, username, passwordEncoder.encode(password));
        dataService.assignRole(tenantId, user.id(), "SUPER_ADMIN");
    }
}
