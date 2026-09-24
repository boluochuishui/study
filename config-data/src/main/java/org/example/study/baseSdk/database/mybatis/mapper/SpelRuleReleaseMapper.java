package org.example.study.baseSdk.database.mybatis.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.example.study.baseSdk.database.mybatis.entity.SpelRuleReleaseEntity;

/** SpEL 规则发布记录 Mapper。 */
public interface SpelRuleReleaseMapper extends BaseMapper<SpelRuleReleaseEntity> {
    List<SpelRuleReleaseEntity> selectByRuleSetCode(@Param("tenantId") long tenantId,
                                                    @Param("ruleSetCode") String ruleSetCode);
    SpelRuleReleaseEntity selectByVersion(@Param("tenantId") long tenantId,
                                          @Param("ruleSetCode") String ruleSetCode,
                                          @Param("releaseVersion") long releaseVersion);
    List<SpelRuleReleaseEntity> selectLatestByScene(@Param("tenantId") long tenantId,
                                                    @Param("scene") String scene);
    List<SpelRuleReleaseEntity> selectChanges(@Param("tenantId") long tenantId,
                                              @Param("scene") String scene,
                                              @Param("afterCursor") long afterCursor, @Param("limit") int limit);
    Long selectMaxCursor(@Param("tenantId") long tenantId, @Param("scene") String scene);
}
