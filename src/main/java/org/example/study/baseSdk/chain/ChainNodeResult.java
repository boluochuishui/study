package org.example.study.baseSdk.chain;

import java.util.List;
import java.util.Map;

/**
 * Structured result returned by one chain handler.
 */
public record ChainNodeResult(
        String handlerName,
        String handlerRole,
        boolean success,
        boolean breakChain,
        String resultCode,
        String message,
        List<String> tags,
        Map<String, Object> details,
        long costMillis
) {

    public static ChainNodeResult success(String handlerName, long costMillis) {
        return new ChainNodeResult(handlerName, "EXECUTE", true, false, "SUCCESS", "success", List.of(), Map.of(), costMillis);
    }

    public static ChainNodeResult risk(String handlerName, String resultCode, String message, List<String> tags, boolean breakChain, long costMillis) {
        return new ChainNodeResult(handlerName, "EXECUTE", true, breakChain, resultCode, message, tags, Map.of(), costMillis);
    }

    public ChainNodeResult withHandlerRole(String role) {
        return new ChainNodeResult(handlerName, role, success, breakChain, resultCode, message, tags, details, costMillis);
    }
}
