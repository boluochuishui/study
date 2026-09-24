package org.example.study.baseSdk.database.mybatis.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.example.study.baseSdk.database.mybatis.entity.SpelRuleSetEntity;

/** SpEL 规则集 Mapper。 */
public interface SpelRuleSetMapper extends BaseMapper<SpelRuleSetEntity> {
    SpelRuleSetEntity selectByCode(@Param("tenantId") long tenantId,
                                   @Param("ruleSetCode") String ruleSetCode);
    SpelRuleSetEntity selectByCodeForUpdate(@Param("tenantId") long tenantId,
                                            @Param("ruleSetCode") String ruleSetCode);
    List<SpelRuleSetEntity> selectAllRuleSets(@Param("tenantId") long tenantId);
}
