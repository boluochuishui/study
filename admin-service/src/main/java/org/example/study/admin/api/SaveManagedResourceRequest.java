package org.example.study.admin.api;

import java.util.Map;
import com.alibaba.fastjson2.annotation.JSONField;

/** 六类业务配置共用的保存请求，字段名与数据库字典一致。 */
public class SaveManagedResourceRequest {
    @JSONField(name = "version")
    private Long version;
    @JSONField(name = "values")
    private Map<String, Object> values;

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public Map<String, Object> getValues() { return values; }
    public void setValues(Map<String, Object> values) { this.values = values; }
}
