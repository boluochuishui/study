package org.example.study.baseSdk.database.mybatis.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/** SpEL 规则发布快照数据库实体。 */
@Getter
@Setter
@TableName("sdk_spel_rule_release")
public class SpelRuleReleaseEntity {
    @TableId
    private Long releaseId;
    private Long tenantId;
    private String ruleSetCode;
    private Long releaseVersion;
    private String scene;
    private String snapshotJson;
    private String checksum;
    private Boolean deleted;
    private String publishedBy;
    private LocalDateTime publishedAt;
}
