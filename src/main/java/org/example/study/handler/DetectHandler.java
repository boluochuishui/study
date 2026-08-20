package org.example.study.handler;

import org.example.study.domain.DetectContext;
import org.example.study.domain.NodeResult;
import org.example.study.strategy.DetectNodeConfig;

public interface DetectHandler {

    String type();

    NodeResult handle(DetectContext context, DetectNodeConfig node);
}
