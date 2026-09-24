package org.example.study.detect.common;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.example.study.baseSdk.chain.ChainExecuteResult;
import org.example.study.domain.ContentType;
import org.example.study.domain.DetectContext;
import org.springframework.stereotype.Component;

/**
 * 按内容类型选择模态执行器，是应用层与具体检测实现之间的稳定边界。
 */
@Component
public class ModalityDetectRouter {

    private final Map<ContentType, ModalityDetectExecutor> executors;

    public ModalityDetectRouter(List<ModalityDetectExecutor> executorList) {
        EnumMap<ContentType, ModalityDetectExecutor> executorMap = new EnumMap<>(ContentType.class);
        for (ModalityDetectExecutor executor : executorList) {
            ModalityDetectExecutor previous = executorMap.put(executor.supportType(), executor);
            if (previous != null) {
                throw new IllegalStateException("Duplicated modality executor: " + executor.supportType());
            }
        }
        this.executors = Map.copyOf(executorMap);
    }

    public ChainExecuteResult execute(ContentType contentType, DetectContext context) {
        ModalityDetectExecutor executor = executors.get(contentType);
        if (executor == null) {
            throw new IllegalArgumentException("Unsupported detection content type: " + contentType);
        }
        return executor.execute(context);
    }
}
