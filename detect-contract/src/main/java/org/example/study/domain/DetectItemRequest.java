package org.example.study.domain;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.io.Serializable;

/** 单样本输入，文本直接传入，媒体原数据使用标准 Base64。 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class DetectItemRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 请求内唯一的样本编号。 */
    @JSONField(name = "itemId")
    private String itemId;

    /** 原始文本内容。 */
    @JSONField(name = "text")
    private String text;

    /** 媒体地址，有原数据时不使用。 */
    @JSONField(name = "url")
    private String url;

    /** 媒体文件的标准 Base64 编码。 */
    @JSONField(name = "data")
    private String data;

    public DetectItemRequest(String itemId, String text, String url, String data) {
        this.itemId = itemId;
        this.text = text;
        this.url = url;
        this.data = data;
    }
}
