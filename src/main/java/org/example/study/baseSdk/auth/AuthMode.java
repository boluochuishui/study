package org.example.study.baseSdk.auth;

/**
 * 系统支持的鉴权模式，后续可增加 TOKEN、INTERNAL 等实现。
 */
public enum AuthMode {
    SIGNATURE,
    ANONYMOUS
}
