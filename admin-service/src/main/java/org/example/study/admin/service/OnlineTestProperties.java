package org.example.study.admin.service;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 凭证只从管理台服务端环境配置加载，不写入租户业务配置。 */
@Component
@ConfigurationProperties(prefix = "admin.online-test")
public class OnlineTestProperties {
    private String accessUrl;
    private Map<String, Credential> credentials = new HashMap<>();

    public String getAccessUrl() {
        return accessUrl;
    }

    public void setAccessUrl(String accessUrl) {
        this.accessUrl = accessUrl;
    }

    public Map<String, Credential> getCredentials() {
        return credentials;
    }

    public void setCredentials(Map<String, Credential> credentials) {
        this.credentials = new HashMap<>(credentials);
    }

    public static class Credential {
        private String appId;
        private String secret;

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }
    }
}
