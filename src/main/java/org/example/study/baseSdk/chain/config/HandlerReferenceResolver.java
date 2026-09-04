package org.example.study.baseSdk.chain.config;

import org.example.study.baseSdk.chain.ChainHandler;
import org.example.study.baseSdk.chain.ChainHandlerRegistry;
import org.example.study.baseSdk.chain.HandlerReference;
import org.example.study.baseSdk.chain.HandlerReferenceType;
import org.springframework.stereotype.Component;

/**
 * Parses and resolves handler references such as bean&name:text.keyword.handler.
 */
@Component
public class HandlerReferenceResolver {

    private static final String BEAN_NAME_PREFIX = "bean&name:";
    private final ChainHandlerRegistry handlerRegistry;

    public HandlerReferenceResolver(ChainHandlerRegistry handlerRegistry) {
        this.handlerRegistry = handlerRegistry;
    }

    public HandlerReference parse(String rawReference) {
        if (rawReference == null || rawReference.isBlank()) {
            throw new IllegalArgumentException("Handler reference must not be blank");
        }
        String trimmed = rawReference.trim();
        if (!trimmed.startsWith(BEAN_NAME_PREFIX)) {
            throw new IllegalArgumentException("Unsupported handler reference: " + rawReference);
        }
        String beanName = trimmed.substring(BEAN_NAME_PREFIX.length()).trim();
        if (beanName.isBlank()) {
            throw new IllegalArgumentException("Handler bean name must not be blank: " + rawReference);
        }
        return new HandlerReference(HandlerReferenceType.BEAN_NAME, beanName);
    }

    public ChainHandler resolve(HandlerReference reference) {
        if (reference.type() != HandlerReferenceType.BEAN_NAME) {
            throw new IllegalArgumentException("Unsupported handler reference type: " + reference.type());
        }
        return handlerRegistry.getRequired(reference.name());
    }
}
