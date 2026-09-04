package org.example.study.application;

import org.example.study.domain.DetectAction;
import org.example.study.domain.DetectRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies multimedia moderation flows assembled from chain properties.
 */
@SpringBootTest
class DetectServiceTests {

    @Autowired
    private DetectService detectService;

    @Test
    void textRequestUsesTextChain() {
        var result = detectService.detect("demo-app", new DetectRequest("task-text", "comment", "TEXT", "正常评论", null, null, null));

        assertThat(result.chainName()).isEqualTo("text.detect.chain");
        assertThat(result.action()).isEqualTo(DetectAction.PASS);
    }

    @Test
    void imageRequestUsesImageChainAndCanRejectOcrRisk() {
        var result = detectService.detect("demo-app", new DetectRequest("task-image", "avatar", "IMAGE", null, "https://example.com/risk-image.png", null, null));

        assertThat(result.chainName()).isEqualTo("image.detect.chain");
        assertThat(result.action()).isEqualTo(DetectAction.REJECT);
        assertThat(result.labels()).contains("keyword_risk");
    }

    @Test
    void audioRequestUsesAudioChainAndCanRejectAsrRisk() {
        var result = detectService.detect("demo-app", new DetectRequest("task-audio", "voice", "AUDIO", null, null, "https://example.com/risk-audio.mp3", null));

        assertThat(result.chainName()).isEqualTo("audio.detect.chain");
        assertThat(result.action()).isEqualTo(DetectAction.REJECT);
        assertThat(result.labels()).contains("keyword_risk");
    }

    @Test
    void videoRequestUsesVideoChainAndCanRejectFrameRisk() {
        var result = detectService.detect("demo-app", new DetectRequest("task-video", "short_video", "VIDEO", null, null, null, "https://example.com/risk-video.mp4"));

        assertThat(result.chainName()).isEqualTo("video.detect.chain");
        assertThat(result.action()).isEqualTo(DetectAction.REJECT);
        assertThat(result.labels()).contains("keyword_risk");
    }
}
