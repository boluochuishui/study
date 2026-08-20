package org.example.study.handler;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class HandlerRegistry {

    private final Map<String, DetectHandler> handlers;

    public HandlerRegistry(List<DetectHandler> handlers) {
        this.handlers = handlers.stream()
                .collect(Collectors.toUnmodifiableMap(DetectHandler::type, Function.identity()));
    }

    public DetectHandler getRequired(String type) {
        DetectHandler handler = handlers.get(type);
        if (handler == null) {
            throw new IllegalArgumentException("Unsupported detect node type: " + type);
        }
        return handler;
    }
}
