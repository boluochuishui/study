package org.example.study.api;

import org.example.study.application.DetectService;
import org.example.study.domain.AsyncDetectAcceptedResult;
import org.example.study.domain.DetectRequest;
import org.example.study.domain.DetectResult;
import org.example.study.domain.ContentType;
import org.example.study.domain.DetectAction;
import org.example.study.domain.DetectItemRequest;
import org.example.study.domain.DetectStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证单项和多项共享检测入口，身份仍来自已鉴权请求头。 */
class DetectControllerTests {

    private final DetectService service = mock(DetectService.class);
    private final DetectController controller = new DetectController(service);

    @Test
    void syncUsesUnifiedItemContractAndHeaderIdentity() {
        var request = request();
        var expected = new DetectResult("task-1", ContentType.TEXT, "COMPLETED", List.of());
        when(service.syncDetect("signed-app", request)).thenReturn(expected);

        assertThat(controller.syncDetect("signed-app", request)).isSameAs(expected);
        verify(service).syncDetect("signed-app", request);
    }

    @Test
    void asyncReturnsAcceptedAfterSubmission() {
        var request = request();
        var expected = new AsyncDetectAcceptedResult("task-2", "request-1", DetectStatus.ACCEPTED, Instant.now());
        when(service.asyncDetect("signed-app", request)).thenReturn(expected);

        var response = controller.asyncDetect("signed-app", request);
        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isSameAs(expected);
    }

    @Test
    void rejectedSubmissionReturnsExplicitDegradedResponse() {
        var request = request();
        var degraded = new AsyncDetectAcceptedResult("task-3", "request-1", DetectStatus.FAILED,
                DetectAction.PASS, Instant.now(), false, true, "DETECT_SERVICE_UNAVAILABLE", "Detection service unavailable");
        when(service.asyncDetect("signed-app", request)).thenReturn(degraded);

        var response = controller.asyncDetect("signed-app", request);
        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(degraded);
    }

    private DetectRequest request() {
        return new DetectRequest("request-1", "comment", "TEXT", "zh-CN", "CN",
                List.of(new DetectItemRequest("one", "normal", null, null)));
    }
}
