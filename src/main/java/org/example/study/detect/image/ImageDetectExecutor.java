package org.example.study.detect.image;

import org.example.study.baseSdk.chain.ChainRuntime;
import org.example.study.detect.common.AbstractModalityDetectExecutor;
import org.example.study.domain.ContentType;
import org.springframework.stereotype.Component;

/**
 * 图片检测执行器。
 */
@Component
public class ImageDetectExecutor extends AbstractModalityDetectExecutor {

    public ImageDetectExecutor(ChainRuntime chainRuntime) {
        super(ContentType.IMAGE, "image.detect.chain", chainRuntime);
    }
}
