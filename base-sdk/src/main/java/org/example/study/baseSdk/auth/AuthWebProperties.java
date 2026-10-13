package org.example.study.baseSdk.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** 鉴权过滤器的 HTTP 接入约束，由接入服务按路径配置。 */
@ConfigurationProperties(prefix = "study.auth")
public class AuthWebProperties {

    private int maxBodyBytes = 67_108_864;
    private List<String> skipPaths = List.of();
    private List<String> jsonOnlyPaths = List.of();

    public int getMaxBodyBytes() {
        return maxBodyBytes;
    }

    public void setMaxBodyBytes(int maxBodyBytes) {
        if (maxBodyBytes <= 0 || maxBodyBytes == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Authentication body limit must be between 1 and 2147483646");
        }
        this.maxBodyBytes = maxBodyBytes;
    }

    public List<String> getSkipPaths() {
        return skipPaths;
    }

    public void setSkipPaths(List<String> skipPaths) {
        this.skipPaths = skipPaths == null ? List.of() : List.copyOf(skipPaths);
    }

    public List<String> getJsonOnlyPaths() {
        return jsonOnlyPaths;
    }

    public void setJsonOnlyPaths(List<String> jsonOnlyPaths) {
        this.jsonOnlyPaths = jsonOnlyPaths == null ? List.of() : List.copyOf(jsonOnlyPaths);
    }
}
