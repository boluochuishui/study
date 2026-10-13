package org.example.study.admin.api;

import java.util.List;

/** 在线测试输入；凭证与场景由服务端配置获取。 */
public record OnlineTestRequest(String contentType, String executionMode, String language,
                                String region, List<OnlineTestItem> items) {
    public OnlineTestRequest {
        if (items != null && items.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("Invalid online test items");
        }
        items = items == null ? null : List.copyOf(items);
    }

    public record OnlineTestItem(String itemId, String text, String url, String data) {
    }
}
