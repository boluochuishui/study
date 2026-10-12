package org.example.study.text.llm;

import org.example.study.domain.DetectAction;

import java.util.List;

/** 大模型返回的供应商无关文本风险结论。 */
public record TextLargeModelAssessment(DetectAction action, List<String> labels) {
    public TextLargeModelAssessment {
        action = action == null ? DetectAction.PASS : action;
        labels = labels == null ? List.of() : List.copyOf(labels);
    }

    public static TextLargeModelAssessment pass() {
        return new TextLargeModelAssessment(DetectAction.PASS, List.of());
    }
}
