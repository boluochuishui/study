package org.example.study.admin;

import java.util.LinkedHashMap;
import org.example.study.admin.api.SaveManagedResourceRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 请求字段的防御拷贝需要保留可选字段的 null 值。 */
class SaveManagedResourceRequestTests {
    @Test
    void isolatesInputAndReturnedMapsWhilePreservingNullValues() {
        var input = new LinkedHashMap<String, Object>();
        input.put("app_name", "Original");
        input.put("description", null);
        var request = new SaveManagedResourceRequest();
        request.setValues(input);

        input.put("app_name", "Changed input");
        var returned = request.getValues();
        assertThat(returned).containsEntry("app_name", "Original").containsEntry("description", null);
        returned.put("app_name", "Changed output");
        returned.remove("description");
        assertThat(request.getValues()).containsEntry("app_name", "Original").containsEntry("description", null);
    }

    @Test
    void preservesMissingValuesForRequestValidation() {
        var request = new SaveManagedResourceRequest();
        assertThat(request.getValues()).isNull();
        request.setValues(null);
        assertThat(request.getValues()).isNull();
    }
}
