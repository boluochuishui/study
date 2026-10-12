package org.example.study.domain;

import java.io.Serializable;
import java.util.List;

/** COMPLETED 只表示整批收尾完成，各项目是否成功由各自状态表达。 */
public record BatchDetectResult(String taskId, ContentType contentType, String status,
                                List<DetectItemResult> results) implements Serializable {
    public BatchDetectResult {
        results = List.copyOf(results);
    }

    public static BatchDetectResult degraded(BatchDetectCommand command, String code, String message) {
        return new BatchDetectResult(command.taskId(), command.contentType(), "COMPLETED",
                command.items().stream().map(item -> DetectItemResult.from(item.itemId(),
                        DetectResult.degradedPass(command.taskId(), code, message))).toList());
    }
}
