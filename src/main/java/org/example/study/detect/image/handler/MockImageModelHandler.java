package org.example.study.detect.image.handler;

import org.example.study.baseSdk.chain.ChainHandler;
import org.example.study.baseSdk.chain.ChainHandlerDefinition;
import org.example.study.baseSdk.chain.ChainNodeResult;
import org.example.study.detect.common.handler.DetectHandlerSupport;
import org.example.study.domain.DetectContext;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * 模拟图片视觉模型审核。
 */
@Component("detect.mock.image.model.handler")
public class MockImageModelHandler extends DetectHandlerSupport implements ChainHandler<DetectContext> {

    @Override
    public ChainNodeResult handle(DetectContext context, ChainHandlerDefinition handlerDefinition) {
        Instant startedAt = Instant.now();
        if (context.getImageUrl() != null && context.getImageUrl().contains("adult")) {
            return reject(handlerDefinition.handlerName(), "mock image model detected adult content", List.of("image_adult"), startedAt);
        }
        return pass(handlerDefinition.handlerName(), startedAt);
    }
}
