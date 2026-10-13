package org.example.study.domain;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;
import java.io.Serializable;

/** 请求汇总结果，COMPLETED 表示处理结束而非全部成功。 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class DetectResult implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 服务端生成的任务编号。 */
    @JSONField(name = "taskId")
    private String taskId;

    /** 本次请求的检测类型。 */
    @JSONField(name = "contentType")
    private ContentType contentType;

    /** 请求处理状态。 */
    @JSONField(name = "status")
    private String status;

    /** 按请求顺序排列的样本结果。 */
    @JSONField(name = "results")
    private List<DetectItemResult> results = List.of();

    public DetectResult(String taskId, ContentType contentType, String status, List<DetectItemResult> results) {
        this.taskId = taskId;
        this.contentType = contentType;
        this.status = status;
        setResults(results);
    }

    public void setResults(List<DetectItemResult> results) {
        this.results = results == null ? List.of() : List.copyOf(results);
    }

    public static DetectResult degraded(DetectCommand command, String code, String message) {
        return new DetectResult(command.taskId(), command.contentType(), "COMPLETED",
                command.items().stream().map(item -> DetectItemResult.degradedPass(item.itemId(), code, message)).toList());
    }
}
