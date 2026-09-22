package org.example.study.detect.image.handler;

import org.example.study.baseSdk.chain.ChainHandler;
import org.example.study.baseSdk.chain.ChainHandlerDefinition;
import org.example.study.baseSdk.chain.ChainNodeResult;
import org.example.study.detect.common.handler.DetectHandlerSupport;
import org.example.study.domain.DetectContext;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 模拟 OCR，并将识别文本写入检测上下文。
 */
@Component("detect.ocr.handler")
public class MockOcrHandler extends DetectHandlerSupport implements ChainHandler<DetectContext> {

    @Override
    public ChainNodeResult handle(DetectContext context, ChainHandlerDefinition handlerDefinition) {
        Instant startedAt = Instant.now();
        if (context.getImageUrl() != null && context.getImageUrl().contains("risk")) {
            context.addDerivedText("OCR识别到诈骗内容");
        } else {
            context.addDerivedText("OCR normal text");
        }
        return pass(handlerDefinition.handlerName(), startedAt);
    }
}
