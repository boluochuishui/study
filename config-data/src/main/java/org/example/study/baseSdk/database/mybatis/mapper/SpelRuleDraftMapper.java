package org.example.study.baseSdk.database.mybatis.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.example.study.baseSdk.database.mybatis.entity.SpelRuleDraftEntity;

/** SpEL 规则草稿 Mapper。 */
public interface SpelRuleDraftMapper extends BaseMapper<SpelRuleDraftEntity> {
    SpelRuleDraftEntity selectByRuleId(@Param("tenantId") long tenantId,
                                       @Param("ruleSetId") long ruleSetId,
                                       @Param("ruleId") String ruleId);
    List<SpelRuleDraftEntity> selectByRuleSetId(@Param("tenantId") long tenantId,
                                                @Param("ruleSetId") long ruleSetId);
}
