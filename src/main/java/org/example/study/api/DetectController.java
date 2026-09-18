package org.example.study.api;

import jakarta.validation.Valid;
import org.example.study.application.DetectService;
import org.example.study.domain.AsyncDetectAcceptedResult;
import org.example.study.domain.DetectRequest;
import org.example.study.domain.DetectResult;
import org.example.study.baseSdk.auth.AuthenticationContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 检测开放接口，同步和异步入口使用不同的执行语义。
 */
@RestController
@RequestMapping("/api/detect")
public class DetectController {

    private final DetectService detectService;

    public DetectController(DetectService detectService) {
        this.detectService = detectService;
    }

    @PostMapping("/sync")
    public DetectResult syncDetect(
            @Valid @RequestBody DetectRequest request
    ) {
        return detectService.syncDetect(AuthenticationContext.requirePrincipal().subject(), request);
    }

    @PostMapping("/async")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AsyncDetectAcceptedResult asyncDetect(
            @Valid @RequestBody DetectRequest request
    ) {
        return detectService.asyncDetect(AuthenticationContext.requirePrincipal().subject(), request);
    }
}
