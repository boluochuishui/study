package org.example.study.baseSdk.chain;

import java.util.Map;

/**
 * 责任链日志上下文，承载整条链路都需要透传的日志字段。
 */
public record ChainLogContext(
        String logSpace,
        String traceId,
        String taskId,
        String appId,
        String sceneCode,
        String chainName,
        String chainVersion,
        Map<String, Object> attributes
) {

    public static ChainLogContext empty(String traceId, String taskId) {
        return new ChainLogContext("content-risk", traceId, taskId, "", "", "", "", Map.of());
    }

    public ChainLogContext withLogSpace(String logSpace) {
        return new ChainLogContext(logSpace, traceId, taskId, appId, sceneCode, chainName, chainVersion, attributes);
    }

    public ChainLogContext withCaller(String appId, String sceneCode) {
        return new ChainLogContext(logSpace, traceId, taskId, appId, sceneCode, chainName, chainVersion, attributes);
    }

    public ChainLogContext withChain(String chainName, String chainVersion) {
        return new ChainLogContext(logSpace, traceId, taskId, appId, sceneCode, chainName, chainVersion, attributes);
    }
}
