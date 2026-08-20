package org.example.study.domain;

import java.util.List;

public record NodeResult(
        String nodeCode,
        String nodeType,
        DetectAction action,
        List<String> labels,
        String reason,
        boolean breakChain,
        long costMillis
) {

    public static NodeResult pass(String nodeCode, String nodeType, long costMillis) {
        return new NodeResult(nodeCode, nodeType, DetectAction.PASS, List.of(), "pass", false, costMillis);
    }
}
