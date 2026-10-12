package org.example.study.text.handler;

import org.example.study.baseSdk.chain.ChainHandler;
import org.example.study.baseSdk.chain.ChainHandlerDefinition;
import org.example.study.baseSdk.chain.ChainNodeResult;
import org.example.study.detect.common.handler.DetectHandlerSupport;
import org.example.study.domain.DetectAction;
import org.example.study.domain.DetectContext;
import org.example.study.text.cache.CachedTextViolation;
import org.example.study.text.cache.TextViolationCache;
import org.example.study.text.config.TextViolationCacheProperties;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** 仅将最终明确拒绝的文本结论写入 Redis。 */
@Component("detect.text.violation.cache.store.handler")
public class TextViolationCacheStoreHandler extends DetectHandlerSupport implements ChainHandler<DetectContext> {

    private final TextViolationCache violationCache;
    private final String decisionVersion;

    public TextViolationCacheStoreHandler(TextViolationCache violationCache,
                                          TextViolationCacheProperties properties) {
        this.violationCache = violationCache;
        this.decisionVersion = properties.getDecisionVersion();
    }

    @Override
    public ChainNodeResult handle(DetectContext context, ChainHandlerDefinition handlerDefinition) {
        Instant startedAt = Instant.now();
        if (context.decision() == DetectAction.REJECT) {
            CachedTextViolation violation = new CachedTextViolation(DetectAction.REJECT,
                    context.decisionLabels(), decisionVersion, Instant.now());
            violationCache.store(context.getAppId(), context.getSceneCode(), context.getContentHash(), violation);
        }
        return pass(handlerDefinition.handlerName(), startedAt);
    }
}
