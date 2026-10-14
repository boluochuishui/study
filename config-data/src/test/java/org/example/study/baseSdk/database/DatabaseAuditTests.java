package org.example.study.baseSdk.database;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.example.study.baseSdk.database.api.audit.DatabaseAuditContext;
import org.example.study.baseSdk.database.mybatis.config.DatabaseMetaObjectHandler;
import org.example.study.baseSdk.database.mybatis.entity.ConfigItemEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 验证线程身份清理及更新不覆盖创建信息。 */
class DatabaseAuditTests {
    @Test
    void restoresNestedIdentityAndDoesNotLeakToAsyncThread() {
        assertThat(DatabaseAuditContext.actorId()).isZero();
        try (var outer = DatabaseAuditContext.open(11)) {
            assertThat(DatabaseAuditContext.actorId()).isEqualTo(11);
            assertThatThrownBy(() -> {
                try (var inner = DatabaseAuditContext.open(12)) {
                    assertThat(DatabaseAuditContext.actorId()).isEqualTo(12);
                    throw new IllegalStateException("Test failure");
                }
            }).isInstanceOf(IllegalStateException.class);
            assertThat(DatabaseAuditContext.actorId()).isEqualTo(11);
            assertThat(CompletableFuture.supplyAsync(DatabaseAuditContext::actorId).join()).isZero();
        }
        assertThat(DatabaseAuditContext.actorId()).isZero();
    }

    @Test
    void insertsAuditFieldsAndRefreshesOnlyUpdateFields() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "audit-test"),
                ConfigItemEntity.class);
        DatabaseMetaObjectHandler handler = new DatabaseMetaObjectHandler();
        ConfigItemEntity entity = new ConfigItemEntity();
        var metaObject = SystemMetaObject.forObject(entity);
        try (var scope = DatabaseAuditContext.open(11)) {
            handler.insertFill(metaObject);
        }
        LocalDateTime createdAt = entity.getCreatedAt();
        assertThat(entity.getCreatedBy()).isEqualTo(11);
        assertThat(entity.getUpdatedBy()).isEqualTo(11);
        assertThat(entity.getUpdatedAt()).isEqualTo(createdAt);
        entity.setUpdatedAt(createdAt.minusDays(1));
        try (var scope = DatabaseAuditContext.open(12)) {
            handler.updateFill(metaObject);
        }
        assertThat(entity.getCreatedBy()).isEqualTo(11);
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getUpdatedBy()).isEqualTo(12);
        assertThat(entity.getUpdatedAt()).isAfterOrEqualTo(createdAt);
    }
}
