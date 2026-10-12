package org.example.study.text.handler;

import org.example.study.baseSdk.chain.ChainHandler;
import org.example.study.baseSdk.chain.ChainHandlerDefinition;
import org.example.study.baseSdk.chain.ChainNodeResult;
import org.example.study.detect.common.handler.DetectHandlerSupport;
import org.example.study.domain.DetectAction;
import org.example.study.domain.DetectContext;
import org.example.study.domain.DetectExecutionMode;
import org.example.study.text.config.TextLargeModelProperties;
import org.example.study.text.llm.TextLargeModelAssessment;
import org.example.study.text.llm.TextLargeModelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** 大模型检测节点，仅允许异步文本任务执行。 */
@Component("detect.text.large.model.handler")
public class TextLargeModelHandler extends DetectHandlerSupport implements ChainHandler<DetectContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TextLargeModelHandler.class);
    private final TextLargeModelClient largeModelClient;
    private final boolean enabled;

    public TextLargeModelHandler(TextLargeModelClient largeModelClient, TextLargeModelProperties properties) {
        this.largeModelClient = largeModelClient;
        this.enabled = properties.isEnabled();
    }

    @Override
    public ChainNodeResult handle(DetectContext context, ChainHandlerDefinition handlerDefinition) {
        Instant startedAt = Instant.now();
        if (!enabled || context.executionMode() != DetectExecutionMode.ASYNC) {
            return pass(handlerDefinition.handlerName(), startedAt);
        }
        try {
            TextLargeModelAssessment assessment = largeModelClient.detect(
                    context.taskId(), context.detectionText());
            if (assessment.action() == DetectAction.PASS) {
                return pass(handlerDefinition.handlerName(), startedAt);
            }
            context.mergeDecision(assessment.action(), assessment.labels());
            return evidence(handlerDefinition.handlerName(), "large model risk matched",
                    assessment.labels(), startedAt);
        } catch (RuntimeException exception) {
            LOGGER.error("Text large model detection failed, taskId={}", context.taskId(), exception);
            return pass(handlerDefinition.handlerName(), startedAt);
        }
    }
}
