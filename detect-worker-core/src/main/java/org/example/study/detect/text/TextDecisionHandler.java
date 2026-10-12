package org.example.study.detect.text.handler;

import org.example.study.baseSdk.chain.ChainHandler;
import org.example.study.baseSdk.chain.ChainHandlerDefinition;
import org.example.study.baseSdk.chain.ChainNodeResult;
import org.example.study.detect.common.handler.DetectHandlerSupport;
import org.example.study.domain.DetectContext;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 将各检测节点累计的证据转换为唯一最终动作。
 */
@Component("detect.text.decision.handler")
public class TextDecisionHandler extends DetectHandlerSupport implements ChainHandler<DetectContext> {

    @Override
    public ChainNodeResult handle(DetectContext context, ChainHandlerDefinition handlerDefinition) {
        Instant startedAt = Instant.now();
        return switch (context.decision()) {
            case REJECT -> reject(handlerDefinition.handlerName(), "text rejected",
                    context.decisionLabels(), startedAt);
            case REVIEW -> review(handlerDefinition.handlerName(), "text requires review",
                    context.decisionLabels(), startedAt);
            case PASS -> pass(handlerDefinition.handlerName(), startedAt);
        };
    }
}
