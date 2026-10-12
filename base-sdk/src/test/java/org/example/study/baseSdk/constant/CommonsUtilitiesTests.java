package org.example.study.baseSdk.constant;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 验证公共工具依赖可用及约定的空值处理行为。 */
class CommonsUtilitiesTests {
    @Test
    void supportsNullSafeStringOperations() {
        assertThat(StringUtils.isBlank(null)).isTrue();
        assertThat(StringUtils.isBlank(" \t\n")).isTrue();
        assertThat(StringUtils.isBlank("text")).isFalse();
        assertThat(Strings.CI.equals("TEXT", "text")).isTrue();
        assertThat(Strings.CS.equals("TEXT", "text")).isFalse();
        assertThat(Strings.CI.equals(null, null)).isTrue();
        assertThat(ObjectUtils.defaultIfNull(null, "default")).isEqualTo("default");
    }

    @Test
    void supportsNullSafeCollectionAndMapOperations() {
        assertThat(CollectionUtils.isEmpty(null)).isTrue();
        assertThat(CollectionUtils.isEmpty(List.of())).isTrue();
        assertThat(CollectionUtils.isNotEmpty(List.of("value"))).isTrue();
        assertThat(MapUtils.isEmpty(null)).isTrue();
        assertThat(MapUtils.isNotEmpty(Map.of("key", "value"))).isTrue();
    }
}
