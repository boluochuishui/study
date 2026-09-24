package org.example.study.baseSdk.database.mybatis.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/** SpEL 规则集数据库实体。 */
@Getter
@Setter
@TableName("sdk_spel_rule_set")
public class SpelRuleSetEntity extends BaseEntity {
    private Long tenantId;
    private String ruleSetCode;
    private String ruleSetName;
    private String scene;
    private String evaluationMode;
    private String errorPolicy;
    private Long publishedVersion;
    private Boolean enabled;
}
