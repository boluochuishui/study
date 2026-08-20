package org.example.study.chain;

import org.example.study.domain.DetectContext;
import org.example.study.domain.DetectResult;
import org.example.study.domain.NodeResult;
import org.example.study.handler.DetectHandler;
import org.example.study.handler.HandlerRegistry;
import org.example.study.strategy.DetectStrategy;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;

@Component
public class DetectChainExecutor {

    private final HandlerRegistry handlerRegistry;

    public DetectChainExecutor(HandlerRegistry handlerRegistry) {
        this.handlerRegistry = handlerRegistry;
    }

    public DetectResult execute(DetectContext context, DetectStrategy strategy) {
        var startedAt = Instant.now();

        for (var node : strategy.nodes().stream().sorted(Comparator.comparingInt(n -> n.order())).toList()) {
            DetectHandler handler = handlerRegistry.getRequired(node.type());
            NodeResult nodeResult = handler.handle(context, node);
            context.addNodeResult(nodeResult);

            if (nodeResult.breakChain()) {
                break;
            }
        }

        return DetectResult.from(context, startedAt, Instant.now());
    }
}
