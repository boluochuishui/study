package org.example.study.baseSdk.hotload;

import java.util.Objects;

/**
 * 单条有序变更，删除事件必须保留键和游标。
 */
public record IncrementalChange<K, V>(
        long cursor,
        ChangeOperation operation,
        K key,
        V value
) {

    public IncrementalChange {
        if (cursor < 0) {
            throw new IllegalArgumentException("Change cursor must not be negative");
        }
        Objects.requireNonNull(operation, "Change operation must not be null");
        Objects.requireNonNull(key, "Change key must not be null");
        if (operation == ChangeOperation.UPSERT) {
            Objects.requireNonNull(value, "Upsert value must not be null");
        }
    }
}
