package org.example.study.detect.video;

import org.example.study.baseSdk.chain.ChainRuntime;
import org.example.study.detect.common.AbstractModalityDetectExecutor;
import org.example.study.domain.ContentType;
import org.springframework.stereotype.Component;

/**
 * 视频检测执行器。
 */
@Component
public class VideoDetectExecutor extends AbstractModalityDetectExecutor {

    public VideoDetectExecutor(ChainRuntime chainRuntime) {
        super(ContentType.VIDEO, "video.detect.chain", chainRuntime);
    }
}
