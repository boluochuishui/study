package org.example.study.handler;

import org.example.study.domain.DetectAction;
import org.example.study.domain.DetectContext;
import org.example.study.domain.NodeResult;
import org.example.study.domain.ContentType;
import org.example.study.strategy.DetectNodeConfig;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class ParamValidateHandler implements DetectHandler {

    @Override
    public String type() {
        return "param_validate";
    }

    @Override
    public NodeResult handle(DetectContext context, DetectNodeConfig node) {
        Instant startedAt = Instant.now();
        ContentType contentType = ContentType.from(context.contentType());
        if (contentType != ContentType.TEXT) {
            return new NodeResult(
                    node.code(),
                    node.type(),
                    DetectAction.REJECT,
                    List.of("invalid_param"),
                    "content type is recognized, but only text detection is implemented in current MVP",
                    true,
                    Duration.between(startedAt, Instant.now()).toMillis()
            );
        }

        if (context.text() == null || context.text().isBlank()) {
            return new NodeResult(
                    node.code(),
                    node.type(),
                    DetectAction.REJECT,
                    List.of("empty_content"),
                    "text must not be blank",
                    true,
                    Duration.between(startedAt, Instant.now()).toMillis()
            );
        }

        return NodeResult.pass(node.code(), node.type(), Duration.between(startedAt, Instant.now()).toMillis());
    }
}
