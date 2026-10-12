package org.example.study.domain;

import java.io.Serializable;

/** 内部项目命令，fileId 仅由接入服务产生，预校验错误保留为逐项结果。 */
public record BatchDetectItem(String itemId, String text, String url, String fileId,
                             String errorCode, String errorMessage) implements Serializable {
}
