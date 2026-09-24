package org.example.study.baseSdk.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.example.study.baseSdk.database.api.exception.ConfigConflictException;
import org.example.study.baseSdk.database.mybatis.entity.SpelRuleReleaseEntity;
import org.example.study.baseSdk.database.mybatis.entity.SpelRuleSetEntity;
import org.example.study.baseSdk.database.mybatis.mapper.SpelRuleDraftMapper;
import org.example.study.baseSdk.database.mybatis.mapper.SpelRuleReleaseMapper;
import org.example.study.baseSdk.database.mybatis.mapper.SpelRuleSetMapper;
import org.example.study.baseSdk.database.mybatis.service.SpelRuleDataServiceImpl;
import org.junit.jupiter.api.Test;

/** 验证规则发布的版本约束和持久化顺序。 */
class SpelRuleDataServiceTests {

    private final SpelRuleSetMapper ruleSetMapper = mock(SpelRuleSetMapper.class);
    private final SpelRuleDraftMapper draftMapper = mock(SpelRuleDraftMapper.class);
    private final SpelRuleReleaseMapper releaseMapper = mock(SpelRuleReleaseMapper.class);
    private final SpelRuleDataServiceImpl service = new SpelRuleDataServiceImpl(
            ruleSetMapper, draftMapper, releaseMapper);

    @Test
    void publishInsertsReleaseAndAdvancesRuleSetVersion() {
        SpelRuleSetEntity ruleSet = ruleSet(5, 2);
        when(ruleSetMapper.selectByCodeForUpdate(1, "text.default")).thenReturn(ruleSet);
        when(releaseMapper.insert(any(SpelRuleReleaseEntity.class))).thenAnswer(invocation -> {
            invocation.<SpelRuleReleaseEntity>getArgument(0).setReleaseId(20L);
            return 1;
        });
        when(ruleSetMapper.updateById(ruleSet)).thenReturn(1);

        var release = service.publish(1, "text.default", 5, 3, "{}", "checksum", false, "admin");

        assertThat(release.releaseId()).isEqualTo(20);
        assertThat(release.releaseVersion()).isEqualTo(3);
        assertThat(release.publishedAt()).isNotNull();
        assertThat(ruleSet.getPublishedVersion()).isEqualTo(3);
        verify(ruleSetMapper).updateById(ruleSet);
    }

    @Test
    void publishRejectsStaleRuleSetVersion() {
        when(ruleSetMapper.selectByCodeForUpdate(1, "text.default")).thenReturn(ruleSet(6, 2));

        assertThatThrownBy(() -> service.publish(
                1, "text.default", 5, 3, "{}", "checksum", false, "admin"))
                .isInstanceOf(ConfigConflictException.class);
    }

    @Test
    void incrementalQueryRejectsUnsafeLimit() {
        assertThatThrownBy(() -> service.listReleaseChanges(1, "TEXT", 0, 501))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid SpEL release cursor query");
    }

    private SpelRuleSetEntity ruleSet(long version, long publishedVersion) {
        SpelRuleSetEntity entity = new SpelRuleSetEntity();
        entity.setId(1L);
        entity.setTenantId(1L);
        entity.setRuleSetCode("text.default");
        entity.setRuleSetName("Default text rules");
        entity.setScene("TEXT");
        entity.setEvaluationMode("ALL_MATCHES");
        entity.setErrorPolicy("SKIP_FAILED");
        entity.setPublishedVersion(publishedVersion);
        entity.setVersion(version);
        entity.setEnabled(true);
        entity.setDeleted(0);
        return entity;
    }
}
