package org.example.study.handler;

import org.example.study.domain.DetectAction;
import org.example.study.domain.DetectContext;
import org.example.study.domain.NodeResult;
import org.example.study.strategy.DetectNodeConfig;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class RegexHandler implements DetectHandler {

    @Override
    public String type() {
        return "regex";
    }

    @Override
    public NodeResult handle(DetectContext context, DetectNodeConfig node) {
        Instant startedAt = Instant.now();
        String text = context.text() == null ? "" : context.text();
        String patternText = node.property("pattern", "");

        if (!patternText.isBlank() && Pattern.compile(patternText).matcher(text).find()) {
            return new NodeResult(
                    node.code(),
                    node.type(),
                    DetectAction.REVIEW,
                    List.of("regex_risk"),
                    "regex pattern matched",
                    false,
                    Duration.between(startedAt, Instant.now()).toMillis()
            );
        }

        return NodeResult.pass(node.code(), node.type(), Duration.between(startedAt, Instant.now()).toMillis());
    }
}
