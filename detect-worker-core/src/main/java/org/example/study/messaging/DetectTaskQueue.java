package org.example.study.messaging;

import org.example.study.domain.DetectTaskMessage;

/** 检测服务内部异步任务队列。 */
public interface DetectTaskQueue {
    boolean publish(DetectTaskMessage message);
}
