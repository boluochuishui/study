package org.example.study.handler;

import org.example.study.domain.DetectAction;
import org.example.study.domain.DetectContext;
import org.example.study.domain.NodeResult;
import org.example.study.strategy.DetectNodeConfig;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Component
public class KeywordHandler implements DetectHandler {

    @Override
    public String type() {
        return "keyword";
    }

    @Override
    public NodeResult handle(DetectContext context, DetectNodeConfig node) {
        Instant startedAt = Instant.now();
        String text = context.text() == null ? "" : context.text();
        List<String> keywords = Arrays.stream(node.property("keywords", "").split(","))
                .map(String::trim)
                .filter(keyword -> !keyword.isEmpty())
                .toList();

        List<String> hits = keywords.stream()
                .filter(text::contains)
                .toList();

        if (hits.isEmpty()) {
            return NodeResult.pass(node.code(), node.type(), elapsed(startedAt));
        }

        return new NodeResult(
                node.code(),
                node.type(),
                DetectAction.REJECT,
                List.of("keyword_risk"),
                "hit keywords: " + String.join(",", hits),
                true,
                elapsed(startedAt)
        );
    }

    private long elapsed(Instant startedAt) {
        return Duration.between(startedAt, Instant.now()).toMillis();
    }
}
