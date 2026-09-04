package org.example.study.handler;

import org.example.study.baseSdk.chain.ChainNodeResult;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Shared helpers for demo moderation handlers.
 */
abstract class DetectHandlerSupport {

    protected long elapsed(Instant startedAt) {
        return Duration.between(startedAt, Instant.now()).toMillis();
    }

    protected ChainNodeResult pass(String handlerName, Instant startedAt) {
        return ChainNodeResult.success(handlerName, elapsed(startedAt));
    }

    protected ChainNodeResult review(String handlerName, String message, List<String> tags, Instant startedAt) {
        return ChainNodeResult.risk(handlerName, "REVIEW", message, tags, false, elapsed(startedAt));
    }

    protected ChainNodeResult reject(String handlerName, String message, List<String> tags, Instant startedAt) {
        return ChainNodeResult.risk(handlerName, "REJECT", message, tags, true, elapsed(startedAt));
    }
}
