package org.example.study.baseSdk.chain.config;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class ChainPropertiesResourceLoader {

    private static final String CHAIN_RESOURCE_PATTERN = "classpath*:/chains/*.chain.properties";

    public List<Resource> load() {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources(CHAIN_RESOURCE_PATTERN);
            return Arrays.stream(resources).toList();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to scan chain properties resources", ex);
        }
    }
}
