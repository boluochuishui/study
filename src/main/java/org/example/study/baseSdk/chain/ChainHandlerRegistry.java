package org.example.study.baseSdk.chain;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Registry of chain handlers keyed by Spring bean name.
 */
@Component
public class ChainHandlerRegistry {

    private final Map<String, ChainHandler> handlers;

    public ChainHandlerRegistry(Map<String, ChainHandler> handlers) {
        this.handlers = Map.copyOf(handlers);
    }

    public ChainHandler getRequired(String beanName) {
        ChainHandler handler = handlers.get(beanName);
        if (handler == null) {
            throw new IllegalArgumentException("Unsupported chain handler bean name: " + beanName);
        }
        return handler;
    }
}
