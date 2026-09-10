package org.example.study.baseSdk.log;

import org.example.study.baseSdk.chain.ChainContext;
import org.example.study.baseSdk.chain.ChainDefinition;
import org.example.study.baseSdk.chain.ChainExecuteMode;
import org.example.study.baseSdk.chain.ChainExceptionPolicy;
import org.example.study.baseSdk.chain.support.BizLogChainExecutionListener;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BaseLogModuleTests {

    @Test
    void shouldDispatchEventToMatchedLogSink() {
        List<BaseLogEvent> apiEvents = new ArrayList<>();
        List<BaseLogEvent> chainEvents = new ArrayList<>();
        LogSink apiSink = new CapturingSink(apiEvents, event -> event.logType() == LogType.API);
        LogSink chainSink = new CapturingSink(chainEvents, event -> event.logType() == LogType.CHAIN);
        CompositeLogRecorder recorder = new CompositeLogRecorder(List.of(apiSink, chainSink));

        recorder.record(new BaseLogEvent(
                "content-risk",
                LogType.API,
                LogLevel.INFO,
                "trace-1",
                "task-1",
                "demo-app",
                "comment",
                "detect-api",
                "API_ACCESS",
                "api access",
                true,
                10,
                Map.of(),
                Instant.now()
        ));

        assertThat(apiEvents).hasSize(1);
        assertThat(chainEvents).isEmpty();
    }

    @Test
    void shouldConvertBizLogEventToBaseLogEvent() {
        CapturingRecorder recorder = new CapturingRecorder();
        DefaultBizLogRecorder bizLogRecorder = new DefaultBizLogRecorder(recorder);

        bizLogRecorder.record(new BizLogEvent(
                "content-risk",
                "trace-1",
                "task-1",
                "demo-app",
                "comment",
                "detect-service",
                "BIZ_DETECT",
                "detect finished",
                true,
                20,
                Map.of("action", "PASS"),
                Instant.now()
        ));

        assertThat(recorder.events).hasSize(1);
        BaseLogEvent event = recorder.events.getFirst();
        assertThat(event.logType()).isEqualTo(LogType.BIZ);
        assertThat(event.logSpace()).isEqualTo("content-risk");
        assertThat(event.attributes()).containsEntry("action", "PASS");
    }

    @Test
    void shouldRecordChainLifecycleAsChainLogType() {
        CapturingRecorder recorder = new CapturingRecorder();
        BizLogChainExecutionListener listener = new BizLogChainExecutionListener(recorder);
        ChainContext context = new ChainContext("trace-1", "task-1");
        context.setLogContext(context.logContext()
                .withLogSpace("content-risk")
                .withCaller("demo-app", "comment")
                .withChain("text.detect.chain", "1.0"));
        ChainDefinition definition = new ChainDefinition(
                "text.detect.chain",
                "1.0",
                ChainExecuteMode.SYNC,
                ChainExceptionPolicy.CONTINUE,
                null,
                List.of(),
                null
        );

        listener.beforeChain(context, definition);

        assertThat(recorder.events).hasSize(1);
        BaseLogEvent event = recorder.events.getFirst();
        assertThat(event.logType()).isEqualTo(LogType.CHAIN);
        assertThat(event.logSpace()).isEqualTo("content-risk");
        assertThat(event.source()).isEqualTo("text.detect.chain");
        assertThat(event.eventType()).isEqualTo("CHAIN_START");
    }

    private record CapturingSink(List<BaseLogEvent> events, java.util.function.Predicate<BaseLogEvent> predicate) implements LogSink {

        @Override
        public boolean supports(BaseLogEvent event) {
            return predicate.test(event);
        }

        @Override
        public void record(BaseLogEvent event) {
            events.add(event);
        }
    }

    private static class CapturingRecorder implements LogRecorder {

        private final List<BaseLogEvent> events = new ArrayList<>();

        @Override
        public void record(BaseLogEvent event) {
            events.add(event);
        }
    }
}
