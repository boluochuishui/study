package org.example.study.text.llm;

/** 文本大模型检测 SPI，具体供应商接入通过实现该接口完成。 */
public interface TextLargeModelClient {

    TextLargeModelAssessment detect(String taskId, String normalizedText);
}
