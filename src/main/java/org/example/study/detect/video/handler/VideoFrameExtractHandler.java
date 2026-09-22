package org.example.study.detect.video.handler;

import org.example.study.baseSdk.chain.ChainHandler;
import org.example.study.baseSdk.chain.ChainHandlerDefinition;
import org.example.study.baseSdk.chain.ChainNodeResult;
import org.example.study.detect.common.handler.DetectHandlerSupport;
import org.example.study.domain.DetectContext;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 模拟从视频中提取代表帧。
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
