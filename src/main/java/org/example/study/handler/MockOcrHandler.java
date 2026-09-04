package org.example.study.handler;

import org.example.study.baseSdk.chain.ChainHandler;
import org.example.study.baseSdk.chain.ChainHandlerDefinition;
import org.example.study.baseSdk.chain.ChainNodeResult;
import org.example.study.domain.DetectContext;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Simulates OCR and pushes extracted text back into the detection context.
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
