package org.example.study.baseSdk.auth;

import org.example.study.baseSdk.log.BaseLogEvent;
import org.example.study.baseSdk.log.LogLevel;
import org.example.study.baseSdk.log.LogRecorder;
import org.example.study.baseSdk.log.LogType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * 记录不包含密钥、Token 和签名原文的结构化鉴权日志。
 */
@Component
public class AuthenticationLogRecorder {

    private final LogRecorder logRecorder;

    public AuthenticationLogRecorder(LogRecorder logRecorder) {
        this.logRecorder = logRecorder;
    }

    public void success(String traceId, String appId, AuthRule rule, String snapshotVersion, long costMillis) {
        record(traceId, appId, rule, snapshotVersion, true, "Authentication succeeded", "", costMillis);
    }

    public void failure(String traceId, String appId, AuthRule rule, String snapshotVersion,
                        AuthenticationException exception, long costMillis) {
        record(traceId, appId, rule, snapshotVersion, false, exception.getMessage(),
                exception.errorCode(), costMillis);
    }

    private void record(String traceId, String appId, AuthRule rule, String snapshotVersion,
                        boolean success, String message, String failureCode, long costMillis) {
        logRecorder.record(new BaseLogEvent(
                "auth",
                LogType.AUTH,
                success ? LogLevel.INFO : LogLevel.WARN,
                traceId,
                "",
                appId,
                "",
                "AuthenticationFilter",
                success ? "AUTH_SUCCESS" : "AUTH_FAILURE",
                message,
                success,
                costMillis,
                Map.of(
                        "ruleId", rule == null ? "" : rule.ruleId(),
                        "authMode", rule == null ? "DENY" : rule.authMode().name(),
                        "snapshotVersion", snapshotVersion,
                        "failureCode", failureCode
                ),
                Instant.now()
        ));
    }
}
