package org.example.study.handler;

import org.example.study.domain.DetectAction;
import org.example.study.domain.DetectContext;
import org.example.study.domain.NodeResult;
import org.example.study.strategy.DetectNodeConfig;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class MockTextModelHandler implements DetectHandler {

    @Override
    public String type() {
        return "mock_text_model";
    }

    @Override
    public NodeResult handle(DetectContext context, DetectNodeConfig node) {
        Instant startedAt = Instant.now();
        String text = context.text() == null ? "" : context.text();

        if (text.length() > Integer.parseInt(node.property("reviewLength", "80"))) {
            return new NodeResult(
                    node.code(),
                    node.type(),
                    DetectAction.REVIEW,
                    List.of("model_review"),
                    "mock model score reached review threshold",
                    false,
                    Duration.between(startedAt, Instant.now()).toMillis()
            );
        }

        return NodeResult.pass(node.code(), node.type(), Duration.between(startedAt, Instant.now()).toMillis());
    }
}
