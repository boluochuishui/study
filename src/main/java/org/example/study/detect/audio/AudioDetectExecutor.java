package org.example.study.detect.audio;

import org.example.study.baseSdk.chain.ChainRuntime;
import org.example.study.detect.common.AbstractModalityDetectExecutor;
import org.example.study.domain.ContentType;
import org.springframework.stereotype.Component;

/**
 * 音频检测执行器。
 */
@Component
public class AudioDetectExecutor extends AbstractModalityDetectExecutor {

    public AudioDetectExecutor(ChainRuntime chainRuntime) {
        super(ContentType.AUDIO, "audio.detect.chain", chainRuntime);
    }
}
