package org.example.study.domain;

/** 单个项目的外部输入，文本、URL 和文件字段引用三选一。 */
public record DetectItemRequest(String itemId, String text, String url, String filePartName) {
}
