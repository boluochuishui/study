package org.example.study.text.handler;

import org.example.study.baseSdk.chain.ChainHandler;
import org.example.study.baseSdk.chain.ChainHandlerDefinition;
import org.example.study.baseSdk.chain.ChainNodeResult;
import org.example.study.detect.common.handler.DetectHandlerSupport;
import org.example.study.domain.DetectContext;
import org.example.study.text.cache.TextViolationCache;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** 命中历史违规样本时直接短路文本检测链。 */
@Component("detect.text.violation.cache.lookup.handler")
public class TextViolationCacheLookupHandler extends DetectHandlerSupport implements ChainHandler<DetectContext> {

    private final TextViolationCache violationCache;

    public TextViolationCacheLookupHandler(TextViolationCache violationCache) {
        this.violationCache = violationCache;
    }

    @Override
    public ChainNodeResult handle(DetectContext context, ChainHandlerDefinition handlerDefinition) {
        Instant startedAt = Instant.now();
        return violationCache.find(context.getAppId(), context.getSceneCode(), context.getContentHash())
                .map(violation -> reject(handlerDefinition.handlerName(), "cached violation matched",
                        violation.labels(), startedAt))
                .orElseGet(() -> pass(handlerDefinition.handlerName(), startedAt));
    }
}
