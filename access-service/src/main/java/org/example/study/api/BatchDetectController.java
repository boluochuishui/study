package org.example.study.api;

import java.util.Map;
import org.example.study.application.BatchDetectService;
import org.example.study.domain.AsyncDetectAcceptedResult;
import org.example.study.domain.BatchDetectRequest;
import org.example.study.domain.BatchDetectResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartHttpServletRequest;

/** 批量协议采用新增入口，保留单条协议不变。 */
@RestController
@RequestMapping("/api/detect/batch")
public class BatchDetectController {
    private final BatchDetectService service;
    public BatchDetectController(BatchDetectService service) { this.service = service; }

    @PostMapping(value = "/sync", consumes = MediaType.APPLICATION_JSON_VALUE)
    public BatchDetectResult sync(@RequestHeader("X-App-Id") String appId, @RequestBody BatchDetectRequest request) {
        return service.sync(appId, request, Map.of());
    }

    @PostMapping(value = "/async", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AsyncDetectAcceptedResult> async(@RequestHeader("X-App-Id") String appId, @RequestBody BatchDetectRequest request) {
        return accepted(service.async(appId, request, Map.of()));
    }

    @PostMapping(value = "/sync", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BatchDetectResult uploadSync(@RequestHeader("X-App-Id") String appId,
                                       @RequestPart("metadata") BatchDetectRequest request, MultipartHttpServletRequest multipart) {
        return service.sync(appId, request, files(multipart));
    }

    @PostMapping(value = "/async", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AsyncDetectAcceptedResult> uploadAsync(@RequestHeader("X-App-Id") String appId,
            @RequestPart("metadata") BatchDetectRequest request, MultipartHttpServletRequest multipart) {
        return accepted(service.async(appId, request, files(multipart)));
    }

    private Map<String, java.util.List<org.springframework.web.multipart.MultipartFile>> files(MultipartHttpServletRequest multipart) {
        var files = new java.util.HashMap<String, java.util.List<org.springframework.web.multipart.MultipartFile>>(multipart.getMultiFileMap());
        files.remove("metadata");
        return files;
    }

    private ResponseEntity<AsyncDetectAcceptedResult> accepted(AsyncDetectAcceptedResult result) {
        return ResponseEntity.status(result.resultExpected() ? HttpStatus.ACCEPTED : HttpStatus.OK).body(result);
    }
}
