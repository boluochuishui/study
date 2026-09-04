package org.example.study.application;

import org.example.study.baseSdk.chain.ChainRuntime;
import org.example.study.domain.DetectContext;
import org.example.study.domain.DetectRequest;
import org.example.study.domain.DetectResult;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Application service that routes requests to the configured content-type chain.
 */
@Service
public class DetectService {

    private final ChainRuntime chainRuntime;

    public DetectService(ChainRuntime chainRuntime) {
        this.chainRuntime = chainRuntime;
    }

    public DetectResult detect(String appId, DetectRequest request) {
        DetectContext context = new DetectContext(UUID.randomUUID().toString(), appId, request);
        return DetectResult.from(chainRuntime.execute(context.getContentType().chainName(), context));
    }
}
