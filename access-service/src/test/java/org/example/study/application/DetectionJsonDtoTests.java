package org.example.study.application;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.annotation.JSONField;
import java.util.List;
import org.example.study.domain.ContentType;
import org.example.study.domain.DetectAction;
import org.example.study.domain.DetectItemRequest;
import org.example.study.domain.DetectItemResult;
import org.example.study.domain.DetectRequest;
import org.example.study.domain.DetectResult;
import org.example.study.domain.DetectStatus;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/** 普通 DTO 的字段映射与 JSON 往返验证。 */
class DetectionJsonDtoTests {
    @Test
    void requestUsesJavaBeanPropertiesAndFastjsonFieldAnnotations() throws Exception {
        var request = new DetectRequest();
        request.setSceneCode("comment");
        request.setContentType("TEXT");
        request.setItems(List.of(new DetectItemRequest("one", "hello", null, null)));
        var copy = JSON.parseObject(JSON.toJSONString(request), DetectRequest.class);
        assertThat(DetectRequest.class.isRecord()).isFalse();
        assertThat(copy).isEqualTo(request);
        assertThat(copy.getItems().getFirst().getText()).isEqualTo("hello");
        assertThat(DetectItemRequest.class.getDeclaredField("data").getAnnotation(JSONField.class).name()).isEqualTo("data");
    }

    @Test
    void responsePreservesWireNamesWithBothJsonLibraries() {
        var item = new DetectItemResult("one", DetectStatus.SUCCEEDED, DetectAction.PASS, List.of(), false, "", "");
        var result = new DetectResult("task", ContentType.TEXT, "COMPLETED", List.of(item));
        var json = JSON.parseObject(JSON.toJSONString(result));
        assertThat(json.getString("taskId")).isEqualTo("task");
        assertThat(json.getJSONArray("results").getJSONObject(0).getBooleanValue("degraded")).isFalse();
        var jackson = new tools.jackson.databind.ObjectMapper();
        assertThat(jackson.readValue(jackson.writeValueAsString(result), DetectResult.class)).isEqualTo(result);
    }
}
