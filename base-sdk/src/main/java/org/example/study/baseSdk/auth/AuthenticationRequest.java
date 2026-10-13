package org.example.study.baseSdk.auth;

import java.util.Map;

/**
 * 与 HTTP、gRPC、Dubbo 等传输协议解耦的认证请求。
 */
public record AuthenticationRequest(
        String method,
        String path,
        Map<String, String> headers,
        byte[] body,
        String remoteAddress,
        String transport
) {
    public AuthenticationRequest {
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        body = body == null ? new byte[0] : body.clone();
    }

    @Override
    public byte[] body() {
        return body.clone();
    }
}
