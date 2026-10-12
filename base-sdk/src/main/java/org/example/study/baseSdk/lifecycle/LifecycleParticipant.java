package org.example.study.baseSdk.lifecycle;

import java.time.Duration;

/** 生命周期扩展点：启动检查、开放入口、摘流和资源收尾。 */
public interface LifecycleParticipant {
    default void prepare() {
    }

    default void onReady() {
    }

    /** 只停止接收新任务，不能提前关闭在途任务使用的资源。 */
    default void onDrain() {
    }

    /** 收尾使用剩余预算；实现必须保证有限等待。 */
    default void afterDrain(Duration remaining) {
    }
}
