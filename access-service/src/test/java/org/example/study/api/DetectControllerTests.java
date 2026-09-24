package org.example.study.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.example.study.application.DetectService;
import org.example.study.domain.AsyncDetectAcceptedResult;
import org.example.study.domain.DetectAction;
import org.example.study.domain.DetectRequest;
import org.example.study.domain.DetectResult;
import org.example.study.domain.DetectStatus;
import org.junit.jupiter.api.Test;

/**
 * 验证检测入口直接将已验签的请求头身份传给应用服务。
 */
class DetectControllerTests {

    private final DetectService detectService = mock(DetectService.class);
    private final DetectController controller = new DetectController(detectService);

    @Test
    void syncUsesAppIdFromRequestHeaderParameter() {
        DetectRequest request = new DetectRequest("request-1", "comment", "TEXT", "normal", null, null, null);
        DetectResult expected = new DetectResult("task-1", DetectStatus.SUCCEEDED, DetectAction.PASS,
                List.of(), false, "", "");
        when(detectService.syncDetect("signed-app", request)).thenReturn(expected);

        assertThat(controller.syncDetect("signed-app", request)).isSameAs(expected);
        verify(detectService).syncDetect("signed-app", request);
    }

    @Test
    void asyncUsesAppIdFromRequestHeaderParameter() {
        DetectRequest request = new DetectRequest("request-2", "voice", "AUDIO", null, null,
                "https://example.com/audio.mp3", null);
        AsyncDetectAcceptedResult expected = new AsyncDetectAcceptedResult(
                "task-2", "request-2", DetectStatus.ACCEPTED, Instant.now());
        when(detectService.asyncDetect("signed-app", request)).thenReturn(expected);

        var response = controller.asyncDetect("signed-app", request);

        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isSameAs(expected);
        verify(detectService).asyncDetect("signed-app", request);
    }

    @Test
    void degradedAsyncRequestReturnsFailOpenResponseInsteadOfAccepted() {
        DetectRequest request = new DetectRequest("request-3", "voice", "AUDIO", null, null,
                "https://example.com/audio.mp3", null);
        AsyncDetectAcceptedResult degraded = new AsyncDetectAcceptedResult(
                "task-3", "request-3", DetectStatus.FAILED, DetectAction.PASS, Instant.now(),
                false, true, "DETECT_SERVICE_UNAVAILABLE", "Detection service unavailable");
        when(detectService.asyncDetect("signed-app", request)).thenReturn(degraded);

        var response = controller.asyncDetect("signed-app", request);

        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(degraded);
    }
}
