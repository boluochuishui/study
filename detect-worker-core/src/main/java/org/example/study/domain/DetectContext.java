package org.example.study.domain;

import lombok.Getter;
import lombok.Setter;
import org.example.study.baseSdk.chain.ChainContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 检测业务上下文，随责任链节点传递。
 */
@Getter
@Setter
public class DetectContext extends ChainContext {

    private final String appId;
    private final String sceneCode;
    private final ContentType contentType;
    private final String text;
    private final String imageUrl;
    private final String audioUrl;
    private final String videoUrl;
    private final DetectExecutionMode executionMode;
    private final List<String> derivedTexts = new ArrayList<>();
    private final Set<String> decisionLabels = new LinkedHashSet<>();
    private String normalizedText = "";
    private String contentHash = "";
    private DetectAction decision = DetectAction.PASS;

    public DetectContext(String traceId, String taskId, String appId, DetectRequest request) {
        super(traceId, taskId);
        this.appId = appId;
        this.sceneCode = request.getSceneCode();
        this.contentType = ContentType.from(request.getContentType());
        this.text = request.getItems().getFirst().getText();
        this.imageUrl = contentType == ContentType.IMAGE ? request.getItems().getFirst().getUrl() : null;
        this.audioUrl = contentType == ContentType.AUDIO ? request.getItems().getFirst().getUrl() : null;
        this.videoUrl = contentType == ContentType.VIDEO ? request.getItems().getFirst().getUrl() : null;
        this.executionMode = DetectExecutionMode.SYNC;
        setLogContext(logContext().withCaller(appId, sceneCode));
    }

    public DetectContext(DetectCommand command) {
        this(command, command.items().getFirst(), command.items().getFirst().url());
    }

    /** 每个样本创建独立上下文，文件引用由 Worker 解析后传入。 */
    public DetectContext(DetectCommand command, DetectItem item, String url) {
        super(command.traceId(), command.taskId());
        this.appId = command.appId();
        this.sceneCode = command.sceneCode();
        this.contentType = command.contentType();
        this.text = item.text();
        this.imageUrl = contentType == ContentType.IMAGE ? url : null;
        this.audioUrl = contentType == ContentType.AUDIO ? url : null;
        this.videoUrl = contentType == ContentType.VIDEO ? url : null;
        this.executionMode = command.executionMode() == null
                ? DetectExecutionMode.SYNC : command.executionMode();
        setLogContext(logContext().withCaller(appId, sceneCode));
    }

    public void addDerivedText(String text) {
        if (text != null && !text.isBlank()) {
            derivedTexts.add(text);
        }
    }

    public List<String> derivedTexts() {
        return List.copyOf(derivedTexts);
    }

    public String allText() {
        StringBuilder builder = new StringBuilder(text == null ? "" : text);
        for (String derivedText : derivedTexts) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(derivedText);
        }
        return builder.toString();
    }

    public String detectionText() {
        return normalizedText.isBlank() ? allText() : normalizedText;
    }

    public DetectExecutionMode executionMode() {
        return executionMode;
    }

    public void mergeDecision(DetectAction action, Collection<String> labels) {
        if (action.ordinal() > decision.ordinal()) {
            decision = action;
        }
        if (labels != null) {
            decisionLabels.addAll(labels);
        }
    }

    public DetectAction decision() {
        return decision;
    }

    public List<String> decisionLabels() {
        return List.copyOf(decisionLabels);
    }
}
