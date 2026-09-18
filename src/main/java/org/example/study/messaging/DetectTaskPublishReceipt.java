package org.example.study.messaging;

/**
 * 消息中间件确认任务写入后的回执。
 */
public record DetectTaskPublishReceipt(
        String messageId
) {
}
