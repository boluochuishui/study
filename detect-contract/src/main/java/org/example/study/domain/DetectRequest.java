package org.example.study.domain;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;
import java.io.Serializable;

/** 统一检测请求，同步单样本，异步文本和图片支持多样本。 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class DetectRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 客户业务关联编号，不作为任务唯一编号。 */
    @JSONField(name = "clientRequestId")
    private String clientRequestId;

    /** 业务场景编码。 */
    @JSONField(name = "sceneCode")
    private String sceneCode;

    /** 本次请求的检测类型。 */
    @JSONField(name = "contentType")
    private String contentType;

    /** 请求语种，可选。 */
    @JSONField(name = "language")
    private String language;

    /** 请求地区，可选。 */
    @JSONField(name = "region")
    private String region;

    /** 待检测样本列表。 */
    @JSONField(name = "items")
    private List<DetectItemRequest> items;

    public DetectRequest(String clientRequestId, String sceneCode, String contentType, String language,
            String region, List<DetectItemRequest> items) {
        this.clientRequestId = clientRequestId;
        this.sceneCode = sceneCode;
        this.contentType = contentType;
        this.language = language;
        this.region = region;
        setItems(items);
    }

    public void setItems(List<DetectItemRequest> items) {
        this.items = items == null ? null : java.util.Collections.unmodifiableList(new java.util.ArrayList<>(items));
    }
}
