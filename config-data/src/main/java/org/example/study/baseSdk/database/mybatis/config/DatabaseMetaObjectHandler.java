package org.example.study.baseSdk.database.mybatis.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;
import org.example.study.baseSdk.database.api.audit.DatabaseAuditContext;

import java.time.LocalDateTime;

/**
 * 自动填充持久化实体的操作人和时间。
 */
@Component
public class DatabaseMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        setFieldValByName("createdBy", DatabaseAuditContext.actorId(), metaObject);
        setFieldValByName("updatedBy", DatabaseAuditContext.actorId(), metaObject);
        strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        setFieldValByName("updatedBy", DatabaseAuditContext.actorId(), metaObject);
        setFieldValByName("updatedAt", LocalDateTime.now(), metaObject);
    }
}
