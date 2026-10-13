package org.example.study.admin.api;

import tools.jackson.databind.JsonNode;

/** 已受理不等于最终检测通过。 */
public record OnlineTestRunResult(String taskId, String status, String executionMode,
                                  long durationMs, JsonNode result) {
}
