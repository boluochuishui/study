package org.example.study.baseSdk.database.mybatis.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/** SpEL 规则草稿数据库实体。 */
@Getter
@Setter
@TableName("sdk_spel_rule_draft")
public class SpelRuleDraftEntity extends BaseEntity {
    private Long tenantId;
    private Long ruleSetId;
    private String ruleId;
    private String ruleName;
    private String expressionText;
    private Integer priority;
    private String attributesJson;
    private Boolean enabled;
}
