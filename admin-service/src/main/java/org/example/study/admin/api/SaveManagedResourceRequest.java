package org.example.study.admin.api;

import java.util.Map;
import java.util.LinkedHashMap;
import com.alibaba.fastjson2.annotation.JSONField;

/** 六类业务配置共用的保存请求，字段名与数据库字典一致。 */
public class SaveManagedResourceRequest {
    @JSONField(name = "version")
    private Long version;
    @JSONField(name = "values")
    private Map<String, Object> values;

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    // 请求允许显式传 null 清空可选字段，因此不能使用 Map.copyOf。
    public Map<String, Object> getValues() {
        return values == null ? null : new LinkedHashMap<>(values);
    }

    public void setValues(Map<String, Object> values) {
        this.values = values == null ? null : new LinkedHashMap<>(values);
    }
}
