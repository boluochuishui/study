package org.example.study.api;

import org.example.study.application.DetectService;
import org.example.study.domain.DetectRequest;
import org.example.study.domain.DetectResult;
import org.example.study.domain.AsyncDetectAcceptedResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 统一检测接入入口，同步仅支持单样本，异步文本和图片支持多样本。
 */
@RestController
@RequestMapping("/api/detect")
public class DetectController {

    private final DetectService detectService;

    public DetectController(DetectService detectService) {
        this.detectService = detectService;
    }

    @PostMapping(value = "/sync", consumes = MediaType.APPLICATION_JSON_VALUE)
    public DetectResult syncDetect(@RequestHeader("X-App-Id") String appId,
                                   @RequestBody DetectRequest request) {
        return detectService.syncDetect(appId, request);
    }

    @PostMapping(value = "/async", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AsyncDetectAcceptedResult> asyncDetect(@RequestHeader("X-App-Id") String appId,
                                                               @RequestBody DetectRequest request) {
        return accepted(detectService.asyncDetect(appId, request));
    }

    private ResponseEntity<AsyncDetectAcceptedResult> accepted(AsyncDetectAcceptedResult result) {
        return ResponseEntity.status(result.isResultExpected() ? HttpStatus.ACCEPTED : HttpStatus.OK).body(result);
    }
}
