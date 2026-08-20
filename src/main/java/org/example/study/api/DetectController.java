package org.example.study.api;

import jakarta.validation.Valid;
import org.example.study.application.DetectService;
import org.example.study.domain.DetectRequest;
import org.example.study.domain.DetectResult;
import org.example.study.security.ApiSignatureVerifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/detect")
public class DetectController {

    private final DetectService detectService;
    private final ApiSignatureVerifier signatureVerifier;

    public DetectController(DetectService detectService, ApiSignatureVerifier signatureVerifier) {
        this.detectService = detectService;
        this.signatureVerifier = signatureVerifier;
    }

    @PostMapping("/sync")
    public DetectResult syncDetect(
            @RequestHeader("X-App-Id") String appId,
            @RequestHeader("X-Timestamp") String timestamp,
            @RequestHeader("X-Nonce") String nonce,
            @RequestHeader("X-Signature") String signature,
            @Valid @RequestBody DetectRequest request
    ) {
        signatureVerifier.verify(appId, timestamp, nonce, signature, request);
        return detectService.detect(request);
    }

    @GetMapping("/scenes")
    public List<String> scenes() {
        return detectService.supportedScenes();
    }
}
