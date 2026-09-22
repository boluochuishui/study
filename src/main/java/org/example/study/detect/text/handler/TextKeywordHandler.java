package org.example.study.detect.text.handler;

import org.example.study.baseSdk.chain.ChainHandler;
import org.example.study.baseSdk.chain.ChainHandlerDefinition;
import org.example.study.baseSdk.chain.ChainNodeResult;
import org.example.study.detect.common.handler.DetectHandlerSupport;
import org.example.study.domain.DetectContext;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * 检测原始文本或派生文本中的高风险关键词。
 */
@Component("detect.text.keyword.handler")
public class TextKeywordHandler extends DetectHandlerSupport implements ChainHandler<DetectContext> {

    @Override
    public ChainNodeResult handle(DetectContext context, ChainHandlerDefinition handlerDefinition) {
        Instant startedAt = Instant.now();
        String text = context.allText();
        List<String> keywords = Arrays.stream("赌博,诈骗,涉黄,暴恐".split(",")).toList();
        List<String> hits = keywords.stream().filter(text::contains).toList();
        if (!hits.isEmpty()) {
            return reject(handlerDefinition.handlerName(), "hit keywords: " + String.join(",", hits), List.of("keyword_risk"), startedAt);
        }
        return pass(handlerDefinition.handlerName(), startedAt);
    }
}
