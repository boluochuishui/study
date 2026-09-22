package org.example.study.detect.common.handler;

import org.example.study.baseSdk.chain.ChainHandler;
import org.example.study.baseSdk.chain.ChainHandlerDefinition;
import org.example.study.baseSdk.chain.ChainNodeResult;
import org.example.study.domain.DetectContext;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 公共收尾节点，为结果整理和后续扩展预留入口。
 */
@Component("detect.finally.handler")
public class DetectFinallyHandler extends DetectHandlerSupport implements ChainHandler<DetectContext> {

    @Override
    public ChainNodeResult handle(DetectContext context, ChainHandlerDefinition handlerDefinition) {
        Instant startedAt = Instant.now();
        context.putAttribute("finalized", true);
        return pass(handlerDefinition.handlerName(), startedAt);
    }
}
