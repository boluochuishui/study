package org.example.study.application;

import org.example.study.baseSdk.chain.ChainContext;
import org.example.study.baseSdk.chain.ChainExecuteResult;
import org.example.study.baseSdk.chain.ChainRuntime;
import org.example.study.baseSdk.chain.ChainSubmitResult;
import org.example.study.domain.DetectAction;
import org.example.study.domain.DetectRequest;
import org.example.study.domain.DetectStatus;
import org.example.study.messaging.DetectTaskPublishReceipt;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证检测执行异常时的故障开放响应。
 */
class DetectServiceDegradedTests {

    @Test
    void syncExecutionFailureReturnsDegradedPass() {
        ChainRuntime failedRuntime = new ChainRuntime() {
            @Override
            public <C extends ChainContext> ChainExecuteResult execute(String chainName, C context) {
                throw new IllegalStateException("Model service unavailable");
            }

            @Override
            public <C extends ChainContext> ChainSubmitResult submit(String chainName, C context) {
                throw new UnsupportedOperationException("Async submit is not used in this test");
            }
        };
        DetectService service = new DetectService(
                failedRuntime,
                new DetectRequestValidator(),
                new TaskIdGenerator(),
                message -> new DetectTaskPublishReceipt("unused"),
                event -> { }
        );

        var result = service.syncDetect(
                "demo-app",
                new DetectRequest("request-1", "comment", "TEXT", "normal text", null, null, null)
        );

        assertThat(result.taskId()).startsWith("dt_");
        assertThat(result.detectStatus()).isEqualTo(DetectStatus.FAILED);
        assertThat(result.action()).isEqualTo(DetectAction.PASS);
        assertThat(result.degraded()).isTrue();
        assertThat(result.errorCode()).isEqualTo("DETECT_EXECUTION_FAILED");
    }
}
