package org.example.study.detect.text;

import org.example.study.baseSdk.chain.ChainRuntime;
import org.example.study.detect.common.AbstractModalityDetectExecutor;
import org.example.study.domain.ContentType;
import org.springframework.stereotype.Component;

/**
 * 文本检测执行器。
 */
@Component
public class TextDetectExecutor extends AbstractModalityDetectExecutor {

    public TextDetectExecutor(ChainRuntime chainRuntime) {
        super(ContentType.TEXT, "text.detect.chain", chainRuntime);
    }
}
