package org.example.study.detect.common;

import org.example.study.baseSdk.chain.ChainExecuteResult;
import org.example.study.domain.ContentType;
import org.example.study.domain.DetectContext;

/**
 * 单一内容模态的检测执行边界。
 */
public interface ModalityDetectExecutor {

    ContentType supportType();

    ChainExecuteResult execute(DetectContext context);
}
