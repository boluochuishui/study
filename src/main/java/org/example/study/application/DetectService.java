package org.example.study.application;

import org.example.study.chain.DetectChainExecutor;
import org.example.study.domain.DetectContext;
import org.example.study.domain.DetectRequest;
import org.example.study.domain.DetectResult;
import org.example.study.strategy.StrategyRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DetectService {

    private final StrategyRepository strategyRepository;
    private final DetectChainExecutor chainExecutor;

    public DetectService(StrategyRepository strategyRepository, DetectChainExecutor chainExecutor) {
        this.strategyRepository = strategyRepository;
        this.chainExecutor = chainExecutor;
    }

    public DetectResult detect(DetectRequest request) {
        var strategy = strategyRepository.getBySceneCode(request.sceneCode());
        var context = new DetectContext(
                request.taskId(),
                request.sceneCode(),
                request.contentType(),
                request.text()
        );
        return chainExecutor.execute(context, strategy);
    }

    public List<String> supportedScenes() {
        return strategyRepository.sceneCodes();
    }
}
