package org.example.study.handler;

import org.example.study.baseSdk.chain.ChainHandler;
import org.example.study.baseSdk.chain.ChainHandlerDefinition;
import org.example.study.baseSdk.chain.ChainNodeResult;
import org.example.study.domain.DetectContext;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Simulates ASR and appends transcription text to the moderation context.
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
