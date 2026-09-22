package org.example.study.baseSdk.hotload;

import java.util.List;

/**
 * 数据来源端口。实现方需保证全量数据和游标来自同一一致性视图。
 */
public interface HotLoadSource<K, V> {

    FullLoadResult<K, V> loadFull();

    /**
     * 按游标升序返回至多 limit 条变更，包含删除事件。
     */
    List<IncrementalChange<K, V>> loadChanges(long afterCursor, int limit);
}
