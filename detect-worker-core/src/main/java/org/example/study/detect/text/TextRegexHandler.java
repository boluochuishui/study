package org.example.study.detect.text.handler;

import org.example.study.baseSdk.chain.ChainHandler;
import org.example.study.baseSdk.chain.ChainHandlerDefinition;
import org.example.study.baseSdk.chain.ChainNodeResult;
import org.example.study.detect.common.handler.DetectHandlerSupport;
import org.example.study.domain.DetectContext;
import org.example.study.domain.DetectAction;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 将包含疑似手机号码的文本标记为人工复审。
 */
@Component("detect.text.regex.handler")
public class TextRegexHandler extends DetectHandlerSupport implements ChainHandler<DetectContext> {

    private static final Pattern PHONE_PATTERN = Pattern.compile("1[3-9]\\d{9}");

    @Override
    public ChainNodeResult handle(DetectContext context, ChainHandlerDefinition handlerDefinition) {
        Instant startedAt = Instant.now();
        if (PHONE_PATTERN.matcher(context.detectionText()).find()) {
            List<String> labels = List.of("contact_risk");
            context.mergeDecision(DetectAction.REVIEW, labels);
            return evidence(handlerDefinition.handlerName(), "phone-like pattern matched", labels, startedAt);
        }
        return pass(handlerDefinition.handlerName(), startedAt);
    }
}
