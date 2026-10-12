package org.example.study.batch;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.example.study.application.BatchDetectionProperties;
import org.example.study.application.BatchRequestPreparer;
import org.example.study.application.UploadedMediaValidator;
import org.example.study.baseSdk.storage.DetectionFileProperties;
import org.example.study.baseSdk.storage.SharedDetectionFileStore;
import org.example.study.domain.BatchDetectRequest;
import org.example.study.domain.DetectExecutionMode;
import org.example.study.domain.DetectItemRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 请求级校验与文件级失败分离，不将错误项目悄悄丢弃。 */
class BatchRequestPreparerTests {
    @TempDir Path directory;
    private final BatchDetectionProperties limits = new BatchDetectionProperties();

    @Test
    void acceptsTextBatchAndRejectsDuplicatesOrExcess() throws Exception {
        var preparer = preparer();
        var item = new DetectItemRequest("one", "hello", null, null);
        assertThat(preparer.prepare(request("TEXT", List.of(item)), DetectExecutionMode.SYNC, Map.of())).hasSize(1);
        assertThatThrownBy(() -> preparer.prepare(request("TEXT", List.of(item, item)), DetectExecutionMode.SYNC, Map.of()))
                .hasMessageContaining("unique");
        limits.setMaxSyncItems(1);
        assertThatThrownBy(() -> preparer.prepare(request("TEXT", List.of(item, new DetectItemRequest("two", "text", null, null))),
                DetectExecutionMode.SYNC, Map.of())).hasMessageContaining("count limit");
    }

    @Test
    void rejectsMixedSourcesAndMediaBatch() throws Exception {
        var preparer = preparer();
        assertThatThrownBy(() -> preparer.prepare(request("IMAGE", List.of(new DetectItemRequest("one", "text", null, null))),
                DetectExecutionMode.SYNC, Map.of())).hasMessageContaining("inline text");
        assertThatThrownBy(() -> preparer.prepare(request("IMAGE", List.of(new DetectItemRequest("one", null, "https://x/a", "file"))),
                DetectExecutionMode.SYNC, Map.of())).hasMessageContaining("exactly one");
        assertThatThrownBy(() -> preparer.prepare(request("AUDIO", List.of(new DetectItemRequest("one", null, "https://x/a", null),
                new DetectItemRequest("two", null, "https://x/b", null))), DetectExecutionMode.ASYNC, Map.of())).hasMessageContaining("count limit");
    }

    @Test
    void keepsValidUploadAndMarksCorruptImageAsItemFailure() throws Exception {
        var preparer = preparer();
        var valid = new MockMultipartFile("img1", "a.png", "image/png", png());
        var bad = new MockMultipartFile("img2", "b.png", "image/png", new byte[]{1, 2, 3});
        var result = preparer.prepare(request("IMAGE", List.of(new DetectItemRequest("one", null, null, "img1"),
                new DetectItemRequest("two", null, null, "img2"))), DetectExecutionMode.SYNC,
                Map.of("img1", List.of(valid), "img2", List.of(bad)));
        assertThat(result.getFirst().fileId()).isNotBlank();
        assertThat(result.getFirst().errorCode()).isEmpty();
        assertThat(result.getLast().fileId()).isNull();
        assertThat(result.getLast().errorCode()).isEqualTo("INVALID_UPLOAD");
    }

    @Test
    void enforcesPixelFileAndTotalTextLimits() throws Exception {
        var preparer = preparer();
        limits.setMaxImagePixels(1);
        var file = new MockMultipartFile("img", "a.png", "image/png", png());
        var image = request("IMAGE", List.of(new DetectItemRequest("one", null, null, "img")));
        assertThat(preparer.prepare(image, DetectExecutionMode.SYNC, Map.of("img", List.of(file))).getFirst().errorCode()).isEqualTo("INVALID_UPLOAD");
        limits.setMaxImageBytes(1);
        assertThat(preparer.prepare(image, DetectExecutionMode.SYNC, Map.of("img", List.of(file))).getFirst().errorCode()).isEqualTo("INVALID_UPLOAD");
        limits.setMaxTotalTextBytes(1);
        assertThatThrownBy(() -> preparer.prepare(request("TEXT", List.of(new DetectItemRequest("one", "hello", null, null))),
                DetectExecutionMode.SYNC, Map.of())).hasMessageContaining("Total text");
    }

    @Test
    void rejectsUnknownFileAndLocalUrl() throws Exception {
        var preparer = preparer();
        assertThatThrownBy(() -> preparer.prepare(request("IMAGE", List.of(new DetectItemRequest("one", null, "file:///etc/passwd", null))),
                DetectExecutionMode.SYNC, Map.of())).hasMessageContaining("Invalid media URL");
        assertThatThrownBy(() -> preparer.prepare(request("TEXT", List.of(new DetectItemRequest("one", "text", null, null))),
                DetectExecutionMode.SYNC, Map.of("extra", List.of(new MockMultipartFile("extra", new byte[]{1})))))
                .hasMessageContaining("Unexpected file");
    }

    static byte[] png() throws Exception {
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", bytes);
        return bytes.toByteArray();
    }

    private BatchDetectRequest request(String type, List<DetectItemRequest> items) {
        return new BatchDetectRequest("client", "comment", type, "zh-CN", "CN", items);
    }

    private BatchRequestPreparer preparer() throws Exception {
        var properties = new DetectionFileProperties();
        properties.setRoot(directory.toString());
        return new BatchRequestPreparer(limits, new SharedDetectionFileStore(properties), new UploadedMediaValidator(limits));
    }
}
