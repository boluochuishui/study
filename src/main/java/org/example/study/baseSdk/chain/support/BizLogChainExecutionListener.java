package org.example.study.baseSdk.chain.support;

import org.example.study.baseSdk.chain.ChainContext;
import org.example.study.baseSdk.chain.ChainDefinition;
import org.example.study.baseSdk.chain.ChainExecuteResult;
import org.example.study.baseSdk.chain.ChainExecutionListener;
import org.example.study.baseSdk.chain.ChainHandlerDefinition;
import org.example.study.baseSdk.chain.ChainNodeResult;
import org.example.study.baseSdk.log.BizLogEvent;
import org.example.study.baseSdk.log.BizLogRecorder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Emits structured logs for chain lifecycle and handler lifecycle events.
 */
@Component
public class BizLogChainExecutionListener implements ChainExecutionListener<ChainContext> {

    private final BizLogRecorder bizLogRecorder;

    public BizLogChainExecutionListener(BizLogRecorder bizLogRecorder) {
        this.bizLogRecorder = bizLogRecorder;
    }

    @Override
    public void beforeChain(ChainContext context, ChainDefinition definition) {
        record(context, "CHAIN_START", "chain started", true, 0, Map.of());
    }

    @Override
    public void beforeHandler(ChainContext context, ChainDefinition definition, ChainHandlerDefinition handlerDefinition, String role) {
        record(context, role + "_START", "handler started", true, 0, handlerAttributes(handlerDefinition, role, null));
    }

    @Override
    public void afterHandler(ChainContext context, ChainDefinition definition, ChainHandlerDefinition handlerDefinition, ChainNodeResult result) {
        record(context, result.handlerRole() + "_END", "handler ended", result.success(), result.costMillis(), handlerAttributes(handlerDefinition, result.handlerRole(), result));
    }

    @Override
    public void onHandlerException(ChainContext context, ChainDefinition definition, ChainHandlerDefinition handlerDefinition, Exception exception) {
        record(context, "HANDLER_EXCEPTION", exception.getMessage(), false, 0, handlerAttributes(handlerDefinition, "EXECUTE", null));
    }

    @Override
    public void afterChain(ChainContext context, ChainDefinition definition, ChainExecuteResult result) {
        record(context, "CHAIN_END", "chain ended", result.success(), result.costMillis(), Map.of("nodeCount", result.nodeResults().size()));
    }

    private void record(ChainContext context, String eventType, String message, boolean success, long costMillis, Map<String, Object> attributes) {
        var logContext = context.logContext();
        bizLogRecorder.record(new BizLogEvent(
                logContext.traceId(),
                logContext.taskId(),
                logContext.appId(),
                logContext.sceneCode(),
                eventType,
                message,
                success,
                costMillis,
                attributes,
                Instant.now()
        ));
    }

    private Map<String, Object> handlerAttributes(ChainHandlerDefinition handlerDefinition, String role, ChainNodeResult result) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("handlerName", handlerDefinition.handlerName());
        attributes.put("handlerRole", role);
        if (result != null) {
            attributes.put("breakChain", result.breakChain());
            attributes.put("resultCode", result.resultCode());
            attributes.put("message", result.message());
        }
        return attributes;
    }
}
