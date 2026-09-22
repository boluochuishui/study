package org.example.study.baseSdk.hotload;

import java.util.Map;

/**
 * 同一数据库一致性视图下读取的全量数据与变更游标。
 */
public record FullLoadResult<K, V>(Map<K, V> items, long cursor) {

    public FullLoadResult {
        if (cursor < 0) {
            throw new IllegalArgumentException("Full load cursor must not be negative");
        }
        items = Map.copyOf(items);
    }
}
