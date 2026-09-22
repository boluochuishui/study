package org.example.study.baseSdk.hotload;

import java.util.Map;

/**
 * 业务模块只负责提供数据来源，并把完整原始数据编译为可读快照。
 */
public interface HotLoadModule<K, V, R> {

    String name();

    HotLoadSource<K, V> source();

    R compile(Map<K, V> items);
}
