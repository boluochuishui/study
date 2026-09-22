package org.example.study.baseSdk.database;

import org.example.study.baseSdk.database.api.exception.ConfigConflictException;
import org.example.study.baseSdk.database.api.model.SaveConfigItemCommand;
import org.example.study.baseSdk.database.mybatis.entity.ConfigItemEntity;
import org.example.study.baseSdk.database.mybatis.mapper.ConfigItemMapper;
import org.example.study.baseSdk.database.mybatis.service.ConfigDataServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证管理台版本条件及并发创建冲突。
 */
class ConfigDataServiceVersionTests {

    private final ConfigItemMapper mapper = mock(ConfigItemMapper.class);
    private final ConfigDataServiceImpl service = new ConfigDataServiceImpl(mapper);
    private final SaveConfigItemCommand command = new SaveConfigItemCommand("demo", "key", "value", true, null);

    @Test
    void createsOnlyWithZeroExpectedVersion() {
        when(mapper.insert(any(ConfigItemEntity.class))).thenReturn(1);

        assertThat(service.saveIfVersion(command, 0).version()).isEqualTo(1);
        verify(mapper).insert(any(ConfigItemEntity.class));
    }

    @Test
    void rejectsMissingItemWhenVersionIsNonZero() {
        assertThatThrownBy(() -> service.saveIfVersion(command, 3))
                .isInstanceOf(ConfigConflictException.class);
    }

    @Test
    void rejectsStaleUpdate() {
        ConfigItemEntity entity = new ConfigItemEntity();
        entity.setId(1L);
        entity.setVersion(4L);
        when(mapper.selectAny("demo", "key")).thenReturn(entity);

        assertThatThrownBy(() -> service.saveIfVersion(command, 3))
                .isInstanceOf(ConfigConflictException.class);
    }

    @Test
    void mapsDuplicateCreateToConflict() {
        when(mapper.insert(any(ConfigItemEntity.class)))
                .thenThrow(new DuplicateKeyException("duplicate"));

        assertThatThrownBy(() -> service.saveIfVersion(command, 0))
                .isInstanceOf(ConfigConflictException.class);
    }
}
