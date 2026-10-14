package org.example.study.baseSdk.database.mybatis.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import org.example.study.baseSdk.database.mybatis.model.ManagedMutation;

/** 六张配置表的 SQL 保存在同名 XML 中。 */
public interface ManagedResourceMapper {
    List<Map<String, Object>> list(@Param("table") String table, @Param("tenantId") long tenantId);

    Map<String, Object> find(@Param("table") String table, @Param("tenantId") long tenantId,
                             @Param("id") long id);

    int insert(@Param("mutation") ManagedMutation mutation);

    int update(@Param("mutation") ManagedMutation mutation);

    Integer hasEnabledReference(@Param("table") String table, @Param("tenantId") long tenantId,
                                @Param("column") String column, @Param("id") long id);
}
