package org.example.study.domain;

import java.util.Objects;

/** 内部任务消息使用统一命令，单样本和多样本不再分别投递。 */
public record DetectTaskMessage(String eventId, DetectCommand command) {
    public DetectTaskMessage {
        Objects.requireNonNull(eventId, "Event identifier must not be null");
        Objects.requireNonNull(command, "Detection command must not be null");
        if (command.executionMode() != DetectExecutionMode.ASYNC) {
            throw new IllegalArgumentException("Task command must use asynchronous execution");
        }
    }

    public String taskId() { return command.taskId(); }
    public ContentType contentType() { return command.contentType(); }
}
