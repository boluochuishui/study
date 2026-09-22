package org.example.study.baseSdk.chain;

/**
 * Parsed handler reference, such as bean&name:xxx.handler.
 */
public record HandlerReference(
        HandlerReferenceType type,
        String name
) {
}
