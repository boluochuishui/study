package org.example.study.messaging;

import org.example.study.domain.DetectResultMessage;

/** 检测结果事件发布边界。 */
public interface DetectResultQueue {
    void publish(DetectResultMessage message);
}
