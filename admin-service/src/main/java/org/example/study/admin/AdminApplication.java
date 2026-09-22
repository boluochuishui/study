package org.example.study.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 独立管理台服务入口。
 */
@SpringBootApplication(scanBasePackages = {"org.example.study.admin", "org.example.study.baseSdk.database"})
public class AdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }
}
