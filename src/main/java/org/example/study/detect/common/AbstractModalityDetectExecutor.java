package org.example.study.detect.common;

import org.example.study.baseSdk.chain.ChainExecuteResult;
import org.example.study.baseSdk.chain.ChainRuntime;
import org.example.study.domain.ContentType;
import org.example.study.domain.DetectContext;

/**
 * 将模态执行器绑定到固定责任链，避免应用层感知链名称。
 */
public abstract class AbstractModalityDetectExecutor implements ModalityDetectExecutor {

    private final ContentType supportType;
    private final String chainName;
    private final ChainRuntime chainRuntime;

    protected AbstractModalityDetectExecutor(ContentType supportType, String chainName, ChainRuntime chainRuntime) {
        this.supportType = supportType;
        this.chainName = chainName;
        this.chainRuntime = chainRuntime;
    }

    @Override
    public ContentType supportType() {
        return supportType;
    }

    @Override
    public ChainExecuteResult execute(DetectContext context) {
        if (context.getContentType() != supportType) {
            throw new IllegalArgumentException("Detection context content type does not match executor");
        }
        return chainRuntime.execute(chainName, context);
    }
}
