package org.example.study.baseSdk.database.mybatis.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * sdk_config_item 表对应的数据库实体，仅在 MyBatis 实现内部使用。
 */
@Getter
@Setter
@TableName("sdk_config_item")
public class ConfigItemEntity extends BaseEntity {

    private String namespace;
    private String configKey;
    private String configValue;
    private Boolean enabled;
    private String description;
}
