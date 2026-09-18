package org.example.study.application;

import org.example.study.domain.DetectAction;
import org.example.study.domain.DetectStatus;
import org.example.study.domain.DetectRequest;
import org.example.study.exception.DetectRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies multimedia moderation flows assembled from chain properties.
 */
@SpringBootTest
class DetectServiceTests {

    @Autowired
    private DetectService detectService;

    @Test
    void textRequestUsesTextChain() {
        var result = detectService.syncDetect("demo-app", new DetectRequest("request-text", "comment", "TEXT", "正常评论", null, null, null));

        assertThat(result.taskId()).startsWith("dt_");
        assertThat(result.detectStatus()).isEqualTo(DetectStatus.SUCCEEDED);
        assertThat(result.action()).isEqualTo(DetectAction.PASS);
        assertThat(result.degraded()).isFalse();
    }

    @Test
    void imageRequestUsesImageChainAndCanRejectOcrRisk() {
        var result = detectService.syncDetect("demo-app", new DetectRequest("request-image", "avatar", "IMAGE", null, "https://example.com/risk-image.png", null, null));

        assertThat(result.action()).isEqualTo(DetectAction.REJECT);
        assertThat(result.labels()).contains("keyword_risk");
    }

    @Test
    void syncRequestRejectsAudio() {
        var request = new DetectRequest("request-audio", "voice", "AUDIO", null, null, "https://example.com/risk-audio.mp3", null);

        assertThatThrownBy(() -> detectService.syncDetect("demo-app", request))
                .isInstanceOf(DetectRequestException.class)
                .hasMessage("Content type AUDIO is not supported in sync detection");
    }

    @Test
    void asyncRequestAcceptsAllFourContentTypesAndGeneratesTaskIds() {
        var text = detectService.asyncDetect("demo-app", new DetectRequest("request-text", "comment", "TEXT", "text", null, null, null));
        var image = detectService.asyncDetect("demo-app", new DetectRequest("request-image", "avatar", "IMAGE", null, "https://example.com/image.png", null, null));
        var audio = detectService.asyncDetect("demo-app", new DetectRequest("request-audio", "voice", "AUDIO", null, null, "https://example.com/audio.mp3", null));
        var video = detectService.asyncDetect("demo-app", new DetectRequest("request-video", "short_video", "VIDEO", null, null, null, "https://example.com/video.mp4"));

        assertThat(text.status()).isEqualTo(DetectStatus.ACCEPTED);
        assertThat(image.status()).isEqualTo(DetectStatus.ACCEPTED);
        assertThat(audio.status()).isEqualTo(DetectStatus.ACCEPTED);
        assertThat(video.status()).isEqualTo(DetectStatus.ACCEPTED);
        assertThat(text.taskId()).startsWith("dt_");
        assertThat(text.taskId()).isNotEqualTo(image.taskId());
        assertThat(image.taskId()).isNotEqualTo(audio.taskId());
        assertThat(audio.taskId()).isNotEqualTo(video.taskId());
    }
}
