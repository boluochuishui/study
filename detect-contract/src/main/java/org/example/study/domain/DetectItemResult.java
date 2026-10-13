package org.example.study.domain;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;
import java.io.Serializable;

/** 单样本公开结果，不暴露责任链轨迹或存储引用。 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class DetectItemResult implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 请求内唯一的样本编号。 */
    @JSONField(name = "itemId")
    private String itemId;

    /** 单样本检测状态。 */
    @JSONField(name = "detectStatus")
    private DetectStatus detectStatus;

    /** 检测处置建议。 */
    @JSONField(name = "action")
    private DetectAction action;

    /** 检测标签集合。 */
    @JSONField(name = "labels")
    private List<String> labels = List.of();

    /** 是否发生异常降级。 */
    @JSONField(name = "degraded")
    private boolean degraded;

    /** 对外错误编码。 */
    @JSONField(name = "errorCode")
    private String errorCode = "";

    /** 英文错误描述。 */
    @JSONField(name = "errorMessage")
    private String errorMessage = "";

    public DetectItemResult(String itemId, DetectStatus detectStatus, DetectAction action, List<String> labels,
            boolean degraded, String errorCode, String errorMessage) {
        this.itemId = itemId;
        this.detectStatus = detectStatus;
        this.action = action;
        setLabels(labels);
        this.degraded = degraded;
        setErrorCode(errorCode);
        setErrorMessage(errorMessage);
    }

    public void setLabels(List<String> labels) {
        this.labels = labels == null ? List.of() : List.copyOf(labels);
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode == null ? "" : errorCode;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage == null ? "" : errorMessage;
    }

    public static DetectItemResult degradedPass(String itemId, String code, String message) {
        return new DetectItemResult(itemId, DetectStatus.FAILED, DetectAction.PASS, List.of(), true, code, message);
    }
}
