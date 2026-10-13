package org.example.study.application;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;
import org.example.study.baseSdk.storage.DetectionFileProperties;
import org.example.study.baseSdk.storage.SharedDetectionFileStore;
import org.example.study.domain.DetectExecutionMode;
import org.example.study.domain.DetectItemRequest;
import org.example.study.domain.DetectRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 仅支持 JSON 输入，原数据优先且无效样本不会回退 URL。 */
class DetectRequestValidatorTests {
    @TempDir Path directory;
    private final DetectionProperties limits = new DetectionProperties();

    @Test
    void synchronousRequestsAllowExactlyOneItemAndAsynchronousIdsMustBeUnique() throws Exception {
        var validator = validator();
        var item = new DetectItemRequest("one", "hello", null, null);
        assertThat(validator.prepare(request("TEXT", List.of(item)), DetectExecutionMode.SYNC)).hasSize(1);
        assertThatThrownBy(() -> validator.prepare(request("TEXT", List.of(item, item)), DetectExecutionMode.ASYNC))
                .hasMessageContaining("unique");
        assertThatThrownBy(() -> validator.prepare(request("TEXT", List.of(item, new DetectItemRequest("two", "text", null, null))),
                DetectExecutionMode.SYNC)).hasMessageContaining("count limit");
    }

    @Test
    void rejectsWrongPayloadAndMultipleAudioSamples() throws Exception {
        var validator = validator();
        assertThatThrownBy(() -> validator.prepare(request("IMAGE", List.of(new DetectItemRequest("one", "text", null, null))),
                DetectExecutionMode.SYNC)).hasMessageContaining("inline text");
        assertThatThrownBy(() -> validator.prepare(request("AUDIO", List.of(new DetectItemRequest("one", null, "https://x/a", null),
                new DetectItemRequest("two", null, "https://x/b", null))), DetectExecutionMode.ASYNC)).hasMessageContaining("count limit");
    }

    @Test
    void checksPixelAndTotalTextLimits() throws Exception {
        var validator = validator();
        limits.setMaxImagePixels(1);
        var image = request("IMAGE", List.of(new DetectItemRequest("one", null, null, Base64.getEncoder().encodeToString(png()))));
        assertThat(validator.prepare(image, DetectExecutionMode.SYNC).getFirst().errorCode()).isEqualTo("INVALID_SAMPLE");
        limits.setMaxTotalTextBytes(1);
        assertThatThrownBy(() -> validator.prepare(request("TEXT", List.of(new DetectItemRequest("one", "hello", null, null))),
                DetectExecutionMode.SYNC)).hasMessageContaining("Total text");
    }

    @Test
    void rejectsLocalUrlsAndAllowsEmptyDataToUseUrl() throws Exception {
        var validator = validator();
        assertThatThrownBy(() -> validator.prepare(request("IMAGE", List.of(new DetectItemRequest("one", null, "file:///etc/passwd", null))),
                DetectExecutionMode.SYNC)).hasMessageContaining("Invalid media URL");
        var input = request("IMAGE", List.of(new DetectItemRequest("one", null, "https://x/a", "")));
        assertThat(validator.prepare(input, DetectExecutionMode.SYNC).getFirst().url()).isEqualTo("https://x/a");
    }

    @Test
    void asynchronousTextHonorsConfiguredItemLimit() throws Exception {
        var validator = validator();
        var items = List.of(new DetectItemRequest("one", "first", null, null), new DetectItemRequest("two", "second", null, null));
        assertThat(validator.prepare(request("TEXT", items), DetectExecutionMode.ASYNC))
                .extracting(item -> item.itemId()).containsExactly("one", "two");
        limits.setMaxAsyncItems(1);
        assertThatThrownBy(() -> validator.prepare(request("TEXT", items), DetectExecutionMode.ASYNC)).hasMessageContaining("count limit");
    }

    @Test
    void base64TakesPriorityOverUrl() throws Exception {
        var input = request("IMAGE", List.of(new DetectItemRequest("one", null, "not-a-url", Base64.getEncoder().encodeToString(png()))));
        var item = validator().prepare(input, DetectExecutionMode.SYNC).getFirst();
        assertThat(item.fileId()).isNotBlank();
        assertThat(item.url()).isNull();
        assertThat(item.errorCode()).isEmpty();
    }

    @Test
    void invalidBase64DoesNotFallBackToUrlOrStopOtherItems() throws Exception {
        var validator = validator();
        for (String data : List.of("%%%", "data:image/png;base64,AAAA", "AAAA ", "AQID")) {
            var input = request("IMAGE", List.of(new DetectItemRequest("one", null, "https://x/a", data),
                    new DetectItemRequest("two", null, "https://x/b", null)));
            var items = validator.prepare(input, DetectExecutionMode.ASYNC);
            assertThat(items.getFirst().errorCode()).isEqualTo("INVALID_SAMPLE");
            assertThat(items.getFirst().fileId()).isNull();
            assertThat(items.getFirst().url()).isNull();
            assertThat(items.getLast().url()).isEqualTo("https://x/b");
            assertThat(items.getLast().errorCode()).isEmpty();
        }
        try (var files = java.nio.file.Files.list(directory)) { assertThat(files.toList()).isEmpty(); }
    }

    @Test
    void enforcesEncodedDecodedAndAggregateSizeLimits() throws Exception {
        var validator = validator();
        limits.setMaxImageBytes(2);
        var input = request("IMAGE", List.of(new DetectItemRequest("one", null, null, "AQID")));
        assertThat(validator.prepare(input, DetectExecutionMode.SYNC).getFirst().errorCode()).isEqualTo("INVALID_SAMPLE");
        var oversized = request("IMAGE", List.of(new DetectItemRequest("one", null, null, "AQIDBA==")));
        assertThatThrownBy(() -> validator.prepare(oversized, DetectExecutionMode.SYNC)).hasMessageContaining("Encoded sample size");
        limits.setMaxRequestBytes(3);
        assertThatThrownBy(() -> validator.prepare(input, DetectExecutionMode.SYNC)).hasMessageContaining("Total sample size");
    }

    static byte[] png() throws Exception {
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", bytes);
        return bytes.toByteArray();
    }

    private DetectRequest request(String type, List<DetectItemRequest> items) {
        return new DetectRequest("client", "comment", type, "zh-CN", "CN", items);
    }

    private DetectRequestValidator validator() throws Exception {
        var properties = new DetectionFileProperties();
        properties.setRoot(directory.toString());
        return new DetectRequestValidator(limits, new SharedDetectionFileStore(properties), new MediaSampleValidator(limits));
    }
}
