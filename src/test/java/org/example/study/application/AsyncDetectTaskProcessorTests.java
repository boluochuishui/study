package org.example.study.application;

import org.example.study.domain.ContentType;
import org.example.study.StudyApplication;
import org.example.study.domain.DetectAction;
import org.example.study.domain.DetectResultMessage;
import org.example.study.domain.DetectStatus;
import org.example.study.domain.DetectTaskMessage;
import org.example.study.messaging.DetectResultPublisher;
import org.example.study.messaging.DetectTaskPublishReceipt;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证异步消费者保留 taskId 并生成终态结果。
 */
@SpringBootTest(classes = {StudyApplication.class, AsyncDetectTaskProcessorTests.TestConfiguration.class})
class AsyncDetectTaskProcessorTests {

    @Autowired
    private AsyncDetectTaskProcessor taskProcessor;

    @Autowired
    private CapturingResultPublisher resultPublisher;

    @Test
    void processorPublishesResultWithOriginalTaskId() {
        DetectTaskMessage message = new DetectTaskMessage(
                "evt-input", "dt-original", "customer-request", "trace-input", "demo-app",
                "comment", ContentType.TEXT, "正常文本", null, null, null, Instant.now()
        );

        taskProcessor.process(message);

        DetectResultMessage result = resultPublisher.messages.getFirst();
        assertThat(result.taskId()).isEqualTo("dt-original");
        assertThat(result.detectStatus()).isEqualTo(DetectStatus.SUCCEEDED);
        assertThat(result.action()).isEqualTo(DetectAction.PASS);
        assertThat(result.degraded()).isFalse();
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfiguration {

        @org.springframework.context.annotation.Bean
        @org.springframework.context.annotation.Primary
        CapturingResultPublisher capturingResultPublisher() {
            return new CapturingResultPublisher();
        }
    }

    static class CapturingResultPublisher implements DetectResultPublisher {
        private final List<DetectResultMessage> messages = new ArrayList<>();

        @Override
        public DetectTaskPublishReceipt publish(DetectResultMessage message) {
            messages.add(message);
            return new DetectTaskPublishReceipt("test-message");
        }
    }
}
