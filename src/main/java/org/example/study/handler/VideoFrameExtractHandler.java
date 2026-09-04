package org.example.study.handler;

import org.example.study.baseSdk.chain.ChainHandler;
import org.example.study.baseSdk.chain.ChainHandlerDefinition;
import org.example.study.baseSdk.chain.ChainNodeResult;
import org.example.study.domain.DetectContext;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Simulates extracting representative frames from video content.
 */
@Component("detect.video.frame.extract.handler")
public class VideoFrameExtractHandler extends DetectHandlerSupport implements ChainHandler<DetectContext> {

    @Override
    public ChainNodeResult handle(DetectContext context, ChainHandlerDefinition handlerDefinition) {
        Instant startedAt = Instant.now();
        context.putAttribute("frameCount", 5);
        if (context.getVideoUrl() != null && context.getVideoUrl().contains("risk")) {
            context.addDerivedText("视频帧OCR包含诈骗内容");
        }
        return pass(handlerDefinition.handlerName(), startedAt);
    }
}
