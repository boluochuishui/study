package org.example.study.baseSdk.chain;

import java.util.Map;

public record ChainLogContext(
        String traceId,
        String taskId,
        String appId,
        String sceneCode,
        String chainName,
        String chainVersion,
        Map<String, Object> attributes
) {

    public static ChainLogContext empty(String traceId, String taskId) {
        return new ChainLogContext(traceId, taskId, "", "", "", "", Map.of());
    }

    public ChainLogContext withChain(String chainName, String chainVersion) {
        return new ChainLogContext(traceId, taskId, appId, sceneCode, chainName, chainVersion, attributes);
    }
}
