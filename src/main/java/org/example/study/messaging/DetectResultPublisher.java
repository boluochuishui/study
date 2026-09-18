package org.example.study.messaging;

import org.example.study.domain.DetectResultMessage;

/**
 * 异步检测终态结果的发布端口。
 */
public interface DetectResultPublisher {

    DetectTaskPublishReceipt publish(DetectResultMessage message);
}
