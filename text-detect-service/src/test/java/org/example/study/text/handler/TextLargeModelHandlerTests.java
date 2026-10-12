package org.example.study.text.handler;

import org.example.study.baseSdk.chain.ChainHandlerDefinition;
import org.example.study.domain.ContentType;
import org.example.study.domain.DetectAction;
import org.example.study.domain.DetectCommand;
import org.example.study.domain.DetectContext;
import org.example.study.domain.DetectExecutionMode;
import org.example.study.text.config.TextLargeModelProperties;
import org.example.study.text.llm.TextLargeModelAssessment;
import org.example.study.text.llm.TextLargeModelClient;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/** 验证大模型能力严格限制在异步文本检测流程。 */
class TextLargeModelHandlerTests {

    @Test
    void skipsLargeModelForSynchronousRequest() {
        AtomicInteger invocationCount = new AtomicInteger();
        TextLargeModelHandler handler = handler(invocationCount);

        var result = handler.handle(context(DetectExecutionMode.SYNC), definition(handler));

        assertThat(result.resultCode()).isEqualTo("SUCCESS");
        assertThat(invocationCount).hasValue(0);
    }

    @Test
    void invokesLargeModelForAsynchronousRequest() {
        AtomicInteger invocationCount = new AtomicInteger();
        TextLargeModelHandler handler = handler(invocationCount);
        DetectContext context = context(DetectExecutionMode.ASYNC);

        var result = handler.handle(context, definition(handler));

        assertThat(result.resultCode()).isEqualTo("EVIDENCE");
        assertThat(invocationCount).hasValue(1);
        assertThat(context.decision()).isEqualTo(DetectAction.REJECT);
        assertThat(context.decisionLabels()).containsExactly("llm_risk");
    }

    private TextLargeModelHandler handler(AtomicInteger invocationCount) {
        TextLargeModelClient client = (taskId, text) -> {
            invocationCount.incrementAndGet();
            return new TextLargeModelAssessment(DetectAction.REJECT, List.of("llm_risk"));
        };
        TextLargeModelProperties properties = new TextLargeModelProperties();
        properties.setEnabled(true);
        return new TextLargeModelHandler(client, properties);
    }

    private DetectContext context(DetectExecutionMode mode) {
        DetectContext context = new DetectContext(new DetectCommand("task", "client", "trace", "app", "scene",
                ContentType.TEXT, "normal text", null, null, null, mode, Instant.now()));
        context.setNormalizedText("normal text");
        return context;
    }

    private ChainHandlerDefinition definition(TextLargeModelHandler handler) {
        return new ChainHandlerDefinition("large-model", null, handler, Map.of());
    }
}
