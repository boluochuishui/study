package org.example.study.domain;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.io.Serializable;

/** 异步任务受理响应，只有入队成功才预期后续结果。 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class AsyncDetectAcceptedResult implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 服务端生成的任务编号。 */
    @JSONField(name = "taskId")
    private String taskId;

    /** 客户业务关联编号，不作为任务唯一编号。 */
    @JSONField(name = "clientRequestId")
    private String clientRequestId;

    /** 请求处理状态。 */
    @JSONField(name = "status")
    private DetectStatus status;

    /** 检测处置建议。 */
    @JSONField(name = "action")
    private DetectAction action;

    /** 任务受理时间。 */
    @JSONField(name = "acceptedAt")
    private Instant acceptedAt;

    /** 是否预期后续结果消息。 */
    @JSONField(name = "resultExpected")
    private boolean resultExpected;

    /** 是否发生异常降级。 */
    @JSONField(name = "degraded")
    private boolean degraded;

    /** 对外错误编码。 */
    @JSONField(name = "errorCode")
    private String errorCode = "";

    /** 英文错误描述。 */
    @JSONField(name = "errorMessage")
    private String errorMessage = "";

    public AsyncDetectAcceptedResult(String taskId, String clientRequestId, DetectStatus status,
            DetectAction action, Instant acceptedAt, boolean resultExpected, boolean degraded, String errorCode,
            String errorMessage) {
        this.taskId = taskId;
        this.clientRequestId = clientRequestId;
        this.status = status;
        this.action = action;
        this.acceptedAt = acceptedAt;
        this.resultExpected = resultExpected;
        this.degraded = degraded;
        setErrorCode(errorCode);
        setErrorMessage(errorMessage);
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode == null ? "" : errorCode;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage == null ? "" : errorMessage;
    }

    public AsyncDetectAcceptedResult(String taskId, String clientRequestId, DetectStatus status, Instant acceptedAt) {
        this(taskId, clientRequestId, status, null, acceptedAt, true, false, "", "");
    }
}
