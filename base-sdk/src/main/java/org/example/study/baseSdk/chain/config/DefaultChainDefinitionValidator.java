package org.example.study.baseSdk.chain.config;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.example.study.baseSdk.chain.ChainDefinition;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 启动时校验责任链名称和已解析的节点。
 */
@Component
public class DefaultChainDefinitionValidator implements ChainDefinitionValidator {

    @Override
    public void validate(List<ChainDefinition> definitions) {
        Set<String> chainNames = new HashSet<>();
        for (ChainDefinition definition : definitions) {
            if (StringUtils.isBlank(definition.chainName())) {
                throw new IllegalArgumentException("Chain name must not be blank");
            }
            if (!chainNames.add(definition.chainName())) {
                throw new IllegalArgumentException("Duplicated chain name: " + definition.chainName());
            }
            if (CollectionUtils.isEmpty(definition.executeHandlers())) {
                throw new IllegalArgumentException("Chain execute.handlers must not be empty: " + definition.chainName());
            }
            definition.executeHandlers().forEach(handlerDefinition -> {
                if (handlerDefinition.handler() == null) {
                    throw new IllegalArgumentException("Chain handler must be resolved: " + handlerDefinition.handlerName());
                }
            });
        }
    }
}
