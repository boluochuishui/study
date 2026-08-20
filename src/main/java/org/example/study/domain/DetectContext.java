package org.example.study.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DetectContext {

    private final String requestId;
    private final String sceneCode;
    private final String contentType;
    private final String text;
    private final List<NodeResult> nodeResults = new ArrayList<>();

    public DetectContext(String requestId, String sceneCode, String contentType, String text) {
        this.requestId = requestId;
        this.sceneCode = sceneCode;
        this.contentType = contentType;
        this.text = text;
    }

    public String requestId() {
        return requestId;
    }

    public String sceneCode() {
        return sceneCode;
    }

    public String contentType() {
        return contentType;
    }

    public String text() {
        return text;
    }

    public void addNodeResult(NodeResult result) {
        nodeResults.add(result);
    }

    public List<NodeResult> nodeResults() {
        return List.copyOf(nodeResults);
    }

    public DetectAction maxRiskAction() {
        return nodeResults.stream()
                .map(NodeResult::action)
                .max(Comparator.comparingInt(Enum::ordinal))
                .orElse(DetectAction.PASS);
    }
}
