package org.example.study.handler;

import org.example.study.baseSdk.chain.ChainHandler;
import org.example.study.baseSdk.chain.ChainHandlerDefinition;
import org.example.study.baseSdk.chain.ChainNodeResult;
import org.example.study.domain.ContentType;
import org.example.study.domain.DetectContext;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Validates that the required payload field exists for the current content type.
 */
@Component("detect.param.validate.handler")
public class ParamValidateHandler extends DetectHandlerSupport implements ChainHandler<DetectContext> {

    @Override
    public ChainNodeResult handle(DetectContext context, ChainHandlerDefinition handlerDefinition) {
        Instant startedAt = Instant.now();
        ContentType type = context.getContentType();
        boolean invalid = switch (type) {
            case TEXT -> isBlank(context.getText());
            case IMAGE -> isBlank(context.getImageUrl());
            case AUDIO -> isBlank(context.getAudioUrl());
            case VIDEO -> isBlank(context.getVideoUrl());
            case MULTIMODAL -> isBlank(context.getText()) && isBlank(context.getImageUrl()) && isBlank(context.getAudioUrl()) && isBlank(context.getVideoUrl());
        };
        if (invalid) {
            return reject(handlerDefinition.handlerName(), "missing content payload for " + type, List.of("invalid_param"), startedAt);
        }
        return pass(handlerDefinition.handlerName(), startedAt);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
