package org.example.study.baseSdk.chain;

/**
 * Strategy used when a chain handler throws an exception.
 */
public enum ChainExceptionPolicy {
    FAIL_FAST,
    CONTINUE,
    BREAK
}
