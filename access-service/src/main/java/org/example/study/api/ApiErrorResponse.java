package org.example.study.api;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

/** 对外统一错误响应，不暴露内部异常堆栈。 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class ApiErrorResponse {
    /** 错误编码。 */
    @JSONField(name = "code")
    private String code;

    /** 英文错误描述。 */
    @JSONField(name = "message")
    private String message;

    /** 事件发生时间。 */
    @JSONField(name = "occurredAt")
    private Instant occurredAt;

    public ApiErrorResponse(String code, String message, Instant occurredAt) {
        this.code = code;
        this.message = message;
        this.occurredAt = occurredAt;
    }
}
