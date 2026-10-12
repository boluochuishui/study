package org.example.study.baseSdk.lifecycle.web;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.example.study.baseSdk.lifecycle.LifecycleProperties;
import org.example.study.baseSdk.lifecycle.ServiceLifecycleManager;
import org.example.study.baseSdk.lifecycle.ServiceState;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 面向容器的健康探针与摘流入口；摘流只允许本机并要求专用令牌。 */
@RestController
@RequestMapping("/internal/lifecycle")
public class LifecycleController {
    private final ServiceLifecycleManager lifecycle;
    private final String drainToken;

    public LifecycleController(ServiceLifecycleManager lifecycle, LifecycleProperties properties) {
        this.lifecycle = lifecycle;
        this.drainToken = properties.getDrainToken();
    }

    @GetMapping("/live")
    public Map<String, String> live() {
        return Map.of("status", "UP");
    }

    @GetMapping("/ready")
    public ResponseEntity<Map<String, String>> ready() {
        boolean ready = lifecycle.state() == ServiceState.READY;
        return ResponseEntity.status(ready ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("state", lifecycle.state().name()));
    }

    @PostMapping("/drain")
    public ResponseEntity<Map<String, String>> drain(HttpServletRequest request,
                                                    @RequestHeader(value = "X-Drain-Token", defaultValue = "") String token) {
        String remote = request.getRemoteAddr();
        boolean loopback = "127.0.0.1".equals(remote) || "::1".equals(remote) || "0:0:0:0:0:0:0:1".equals(remote);
        if (!loopback || drainToken == null || drainToken.isBlank()
                || !MessageDigest.isEqual(drainToken.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Drain access denied"));
        }
        lifecycle.beginDrain();
        return ResponseEntity.ok(Map.of("state", lifecycle.state().name()));
    }
}
