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
 * Detection-specific context passed across moderation chain handlers.
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
        this.sceneCode = request.sceneCode();
        this.contentType = ContentType.from(request.contentType());
        this.text = request.text();
        this.imageUrl = request.imageUrl();
        this.audioUrl = request.audioUrl();
        this.videoUrl = request.videoUrl();
        this.executionMode = DetectExecutionMode.SYNC;
        setLogContext(logContext().withCaller(appId, sceneCode));
    }

    public DetectContext(DetectCommand command) {
        super(command.traceId(), command.taskId());
        this.appId = command.appId();
        this.sceneCode = command.sceneCode();
        this.contentType = command.contentType();
        this.text = command.text();
        this.imageUrl = command.imageUrl();
        this.audioUrl = command.audioUrl();
        this.videoUrl = command.videoUrl();
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
