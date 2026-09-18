package org.example.study.messaging;

import org.example.study.domain.DetectTaskMessage;

/**
 * 异步检测任务的可靠发布端口。
 */
public interface DetectTaskPublisher {

    DetectTaskPublishReceipt publish(DetectTaskMessage message);
}
