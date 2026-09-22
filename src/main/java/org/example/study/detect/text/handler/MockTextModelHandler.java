package org.example.study.detect.text.handler;

import org.example.study.baseSdk.chain.ChainHandler;
import org.example.study.baseSdk.chain.ChainHandlerDefinition;
import org.example.study.baseSdk.chain.ChainNodeResult;
import org.example.study.detect.common.handler.DetectHandlerSupport;
import org.example.study.domain.DetectContext;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * 模拟文本模型的审核决策。
 */
@Component("detect.mock.text.model.handler")
public class MockTextModelHandler extends DetectHandlerSupport implements ChainHandler<DetectContext> {

    @Override
    public ChainNodeResult handle(DetectContext context, ChainHandlerDefinition handlerDefinition) {
        Instant startedAt = Instant.now();
        if (context.allText().length() > 80) {
            return review(handlerDefinition.handlerName(), "mock text model reached review threshold", List.of("model_review"), startedAt);
        }
        return pass(handlerDefinition.handlerName(), startedAt);
    }
}
