package org.example.study.baseSdk.lifecycle;

/** 服务从初始化到退出的单向生命周期状态。 */
public enum ServiceState {
    STARTING, READY, DRAINING, STOPPED
}
