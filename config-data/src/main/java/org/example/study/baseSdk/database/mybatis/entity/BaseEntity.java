package org.example.study.baseSdk.database.mybatis.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Getter;
import lombok.Setter;

/**
 * MyBatis 持久化实体的通用审计字段。
 */
@Getter
@Setter
public abstract class BaseEntity extends AuditedEntity {

    @TableId
    private Long id;

    @Version
    private Long version;

    @TableLogic
    private Integer deleted;
}
