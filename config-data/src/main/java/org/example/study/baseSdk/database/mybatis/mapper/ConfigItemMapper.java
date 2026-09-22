package org.example.study.baseSdk.database.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.example.study.baseSdk.database.mybatis.entity.ConfigItemEntity;

import java.util.List;

/**
 * 通用配置项 Mapper，自定义 SQL 统一保存在对应 XML 文件中。
 */
public interface ConfigItemMapper extends BaseMapper<ConfigItemEntity> {

    ConfigItemEntity selectEnabled(
            @Param("namespace") String namespace,
            @Param("configKey") String configKey
    );

    ConfigItemEntity selectAny(
            @Param("namespace") String namespace,
            @Param("configKey") String configKey
    );

    List<ConfigItemEntity> selectEnabledList(@Param("namespace") String namespace);

    List<ConfigItemEntity> selectListByNamespace(@Param("namespace") String namespace);
}
