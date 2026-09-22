package org.example.study.detect.audio.handler;

import org.example.study.baseSdk.chain.ChainHandler;
import org.example.study.baseSdk.chain.ChainHandlerDefinition;
import org.example.study.baseSdk.chain.ChainNodeResult;
import org.example.study.detect.common.handler.DetectHandlerSupport;
import org.example.study.domain.DetectContext;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 模拟 ASR，并将转写文本写入检测上下文。
 */
@Component("detect.asr.handler")
public class MockAsrHandler extends DetectHandlerSupport implements ChainHandler<DetectContext> {

    @Override
    public ChainNodeResult handle(DetectContext context, ChainHandlerDefinition handlerDefinition) {
        Instant startedAt = Instant.now();
        if (context.getAudioUrl() != null && context.getAudioUrl().contains("risk")) {
            context.addDerivedText("ASR转写包含赌博内容");
        } else {
            context.addDerivedText("ASR normal text");
        }
        return pass(handlerDefinition.handlerName(), startedAt);
    }
}
